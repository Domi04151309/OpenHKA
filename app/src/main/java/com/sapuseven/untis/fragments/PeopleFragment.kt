package com.sapuseven.untis.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.PeopleAdapter
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
	private var loading = true
	private val keyMap: MutableMap<String, JSONObject> = mutableMapOf()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/hskampus-broker/api"
		private const val FRAGMENT_TAG_PEOPLE: String = "com.sapuseven.untis.fragments.people"
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(
			R.layout.fragment_infocenter,
			container,
			false
		)

		stringLoader = StringLoader(WeakReference(context), this, "${API_URL}/persons")
		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = adapter
		swiperefreshlayout.isRefreshing = loading
		swiperefreshlayout.setOnRefreshListener { refreshEvents(StringLoader.FLAG_LOAD_SERVER) }

		refreshEvents(StringLoader.FLAG_LOAD_CACHE)

		//TODO: add onClick
		adapter.onClickListener = View.OnClickListener {
			val fragment = PeopleDetailsFragment(
				keyMap[it.findViewById<TextView>(R.id.textview_itemmessage_subject).text]
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

	private fun refreshEvents(flags: Int) {
		loading = true
		stringLoader.load(flags)
	}

	//TODO: doesn't work if two people have the same name; use ids
	override fun onStringLoaded(string: String) {
		list.clear()
		val treeMap = TreeMap<String, PeopleListItem>()
		val json = JSONArray(string)
		var currentObject: JSONObject
		var title: String
		for (i in 0 until json.length()) {
			currentObject = json.getJSONObject(i)
			title = currentObject.optString("lastName") + ", " +
					currentObject.optString("firstName")
			treeMap[title] = PeopleListItem(
				currentObject.optString("academicDegree"),
				title,
				currentObject.optString("email") + " | " +
						currentObject.optString("faculty")
			)
			keyMap[title] = currentObject
		}
		list.addAll(treeMap.values)
		adapter.notifyDataSetChanged()
		loading = false
		swiperefreshlayout.isRefreshing = false
	}

	override fun onStringLoadingError(code: Int) {
		when (code) {
			StringLoader.CODE_CACHE_MISSING -> stringLoader.repeat(
				StringLoader.FLAG_LOAD_SERVER
			)
			else -> {
				Toast.makeText(
					context,
					R.string.errors_failed_loading_from_server_message,
					Toast.LENGTH_LONG
				).show()
				loading = false
				swiperefreshlayout.isRefreshing = false
			}
		}
	}
}
