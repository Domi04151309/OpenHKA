package com.sapuseven.untis.fragments

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.MessageAdapter
import com.sapuseven.untis.data.GenericParseResult
import com.sapuseven.untis.data.lists.ListItem
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference


class LocationFragment : Fragment(), StringDisplay {
	private val adapter = MessageAdapter()
	private var parsedData: GenericParseResult<ListItem, Pair<Double, Double>> = GenericParseResult()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"

		fun parseLocations(resources: Resources, input: String): GenericParseResult<ListItem, Pair<Double, Double>> {
			val result = GenericParseResult<ListItem, Pair<Double, Double>>()
			val json = JSONArray(input)
			var departments: JSONArray
			var currentBuilding: JSONObject
			var currentDepartment: String
			for (i in 0 until json.length()) {
				currentBuilding = json.getJSONObject(i)
				departments = currentBuilding.optJSONArray("departments") ?: JSONArray()
				if (departments.length() > 0) result.list.add(
					ListItem(
						"",
						resources.getString(
							R.string.locations_building,
							currentBuilding.optString("name")
						)
					)
				)
				for (j in 0 until departments.length()) {
					currentDepartment = departments.getJSONObject(j).optString("name")
					result.list.add(
						ListItem(
							currentDepartment,
							""
						)
					)
					result.map[currentDepartment] = Pair(
						currentBuilding.optDouble("latitude"),
						currentBuilding.optDouble("longitude")
					)
				}
			}
			return result
		}
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

		stringLoader = StringLoader(WeakReference(context), this, "${API_URL}/buildings/v2/all")
		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = adapter
		swiperefreshlayout.setOnRefreshListener { refreshLocations(StringLoader.FLAG_LOAD_SERVER) }

		refreshLocations(StringLoader.FLAG_LOAD_CACHE)

		adapter.onClickListener = View.OnClickListener {
			val key = it.findViewById<TextView>(R.id.textview_itemmessage_subject).text.toString()
			if (key.isNotEmpty()) {
				val mapIntent = Intent(
					Intent.ACTION_VIEW, Uri.parse(
						"geo:0,0?q=${parsedData.map[key]?.first},${parsedData.map[key]?.second}"
					)
				)
				mapIntent.setPackage("com.google.android.apps.maps")
				startActivity(mapIntent)
			}
		}

		return root
	}

	private fun refreshLocations(flags: Int) {
		swiperefreshlayout.isRefreshing = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		parsedData = parseLocations(resources, string)
		adapter.updateItems(parsedData.list)
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
