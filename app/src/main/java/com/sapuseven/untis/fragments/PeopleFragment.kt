package com.sapuseven.untis.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.PeopleAdapter
import com.sapuseven.untis.data.GenericParseResult
import com.sapuseven.untis.data.lists.PeopleListItem
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*


class PeopleFragment : Fragment(), StringDisplay {
	private val list = arrayListOf<PeopleListItem>()
	private val adapter = PeopleAdapter(list)
	private var parsedData: GenericParseResult<PeopleListItem, JSONObject> = GenericParseResult()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/hskampus-broker/api"
		private const val FRAGMENT_TAG_PEOPLE: String = "com.sapuseven.untis.fragments.people"

		//TODO: doesn't work if two people have the same name; use ids
		fun parsePeople(input: String): GenericParseResult<PeopleListItem, JSONObject> {
			val treeMap = TreeMap<String, PeopleListItem>()
			val map = mutableMapOf<String, JSONObject>()
			val json = JSONArray(input)
			var currentObject: JSONObject
			var title: String
			for (i in 0 until json.length()) {
				currentObject = json.getJSONObject(i)
				if (!currentObject.optBoolean("deleted")) {
					title = currentObject.optString("lastName") + ", " +
							currentObject.optString("firstName")
					treeMap[title] = PeopleListItem(
						currentObject.optString("imageUrl"),
						currentObject.optString("academicDegree"),
						title,
						currentObject.optString("email") + " | " +
								currentObject.optString("faculty")
					)
					map[title] = currentObject
				}
			}
			return GenericParseResult(ArrayList(treeMap.values), map)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(
			R.layout.fragment_people,
			container,
			false
		)

		stringLoader = StringLoader(WeakReference(context), this, "${API_URL}/persons")
		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = adapter
		swiperefreshlayout.setOnRefreshListener { refreshEvents(StringLoader.FLAG_LOAD_SERVER) }

		refreshEvents(StringLoader.FLAG_LOAD_CACHE)

		adapter.onClickListener = View.OnClickListener {
			val fragment = PeopleDetailsFragment(
				parsedData.map[it.findViewById<TextView>(R.id.textview_itemmessage_subject).text]
					?: return@OnClickListener
			)
			(activity as AppCompatActivity).supportFragmentManager.beginTransaction().run {
				setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				add(R.id.content_main, fragment, FRAGMENT_TAG_PEOPLE)
				addToBackStack(fragment.tag)
				commit()
			}
		}

		return root
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.activity_people_menu, menu)

		(menu.findItem(R.id.search).actionView as SearchView)
			.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
				override fun onQueryTextSubmit(query: String): Boolean {
					return this.onQueryTextChange(query)
				}

				override fun onQueryTextChange(newText: String): Boolean {
					list.clear()
					list.addAll(parsedData.list.filter {
						it.title.lowercase().contains(newText.lowercase())
					})
					adapter.notifyDataSetChanged()
					return true
				}
			})
	}


	private fun refreshEvents(flags: Int) {
		swiperefreshlayout.isRefreshing = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		parsedData = parsePeople(string)
		list.clear()
		list.addAll(parsedData.list)
		adapter.notifyDataSetChanged()
		swiperefreshlayout.isRefreshing = false
	}

	override fun onStringLoadingError(code: Int, loader: StringLoader) {
		when (code) {
			StringLoader.CODE_CACHE_MISSING -> loader.repeat(
				StringLoader.FLAG_LOAD_SERVER
			)
			else -> {
				Toast.makeText(
					context,
					R.string.errors_failed_loading_from_server_message,
					Toast.LENGTH_LONG
				).show()
				swiperefreshlayout.isRefreshing = false
			}
		}
	}
}
