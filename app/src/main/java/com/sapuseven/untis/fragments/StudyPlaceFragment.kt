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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.StudyPlaceAdapter
import com.sapuseven.untis.data.GenericParseResult
import com.sapuseven.untis.data.lists.StudyPlaceListItem
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference


class StudyPlaceFragment : Fragment(), StringDisplay {
	private val adapter = StudyPlaceAdapter()
	private var parsedData: GenericParseResult<StudyPlaceListItem, Pair<Double, Double>> =
		GenericParseResult()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/hskampus-broker/api"

		private fun parseStudyPlacesIntern(
			resources: Resources,
			places: String
		): GenericParseResult<StudyPlaceListItem, Pair<Double, Double>> {
			val result = GenericParseResult<StudyPlaceListItem, Pair<Double, Double>>()
			val json = JSONArray(places)
			val array = Array(json.length()) { StudyPlaceListItem(0, 0, "", "", "") }
			var currentItem: JSONObject
			var currentTitle: String
			for (i in 0 until json.length()) {
				currentItem = json.getJSONObject(i)
				currentTitle = currentItem.optString("longName").replace(", ", ",\n")
				array[currentItem.optInt("id") - 1] = StudyPlaceListItem(
					0,
					currentItem.optInt("availableSeats"),
					resources.getString(R.string.study_places_occupation_unknown),
					currentTitle,
					currentItem.optString("openingHours")
				)
				result.map[currentTitle] = Pair(
					currentItem.optDouble("latitude"),
					currentItem.optDouble("longitude")
				)
			}
			result.list = ArrayList(array.toMutableList())
			return result
		}

		fun parseStudyPlaces(
			resources: Resources,
			places: String
		): GenericParseResult<StudyPlaceListItem, Pair<Double, Double>> {
			val result = parseStudyPlacesIntern(resources, places)
			result.list.sortBy { it.title }
			return result
		}

		fun parseStudyPlaces(
			resources: Resources,
			places: String,
			occupations: String
		): GenericParseResult<StudyPlaceListItem, Pair<Double, Double>> {
			val result = parseStudyPlacesIntern(resources, places)
			val json = JSONArray(occupations)
			var currentItem: JSONObject
			for (i in 0 until json.length()) {
				currentItem = json.getJSONObject(i)
				result.list[currentItem.optInt("id") - 1].let {
					it.value = currentItem.optInt("occupiedSeats")
					it.overline = resources.getString(
						R.string.study_places_occupation, it.value, it.max
					)
				}
			}
			result.list.sortBy { it.title }
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

		stringLoader = StringLoader(WeakReference(context), this, "${API_URL}/learningPlaces")
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
		val callback = object : StringDisplay {
			override fun onStringLoaded(innerString: String) {
				parsedData = parseStudyPlaces(resources, string, innerString)
				adapter.updateItems(parsedData.list)
				swiperefreshlayout.isRefreshing = false
			}

			override fun onStringLoadingError(code: Int, loader: StringLoader) {
				Toast.makeText(
					context,
					R.string.errors_failed_loading_from_server_message,
					Toast.LENGTH_LONG
				).show()
				parsedData = parseStudyPlaces(resources, string)
				adapter.updateItems(parsedData.list)
				swiperefreshlayout.isRefreshing = false
			}
		}
		StringLoader(WeakReference(context), callback, "${API_URL}/learningPlaceOccupations").load(
			StringLoader.FLAG_LOAD_SERVER
		)
	}

	override fun onStringLoadingError(code: Int, loader: StringLoader) {
		when (code) {
			StringLoader.CODE_CACHE_MISSING -> loader.repeat(
				StringLoader.FLAG_LOAD_SERVER
			)
			else -> {
				MaterialAlertDialogBuilder(requireContext())
					.setTitle(R.string.activity_title_study_places)
					.setMessage(R.string.errors_failed_loading_from_server_message)
					.setPositiveButton(R.string.all_ok) { _, _ -> }
					.show()
				swiperefreshlayout.isRefreshing = false
			}
		}
	}
}
