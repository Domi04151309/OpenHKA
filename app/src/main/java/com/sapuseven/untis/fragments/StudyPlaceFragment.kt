package com.sapuseven.untis.fragments

import android.content.Intent
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
import com.sapuseven.untis.data.lists.StudyPlaceListItem
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference


class StudyPlaceFragment : Fragment(), StringDisplay {
	private val locationList = arrayListOf<StudyPlaceListItem>()
	private val locationAdapter = StudyPlaceAdapter(locationList)
	private var locationsLoading = true
	private val keyMap: MutableMap<String, Pair<Double, Double>> = mutableMapOf()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/hskampus-broker/api"
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
		recyclerview.adapter = locationAdapter
		swiperefreshlayout.isRefreshing = locationsLoading
		swiperefreshlayout.setOnRefreshListener { refreshLocations(StringLoader.FLAG_LOAD_SERVER) }

		refreshLocations(StringLoader.FLAG_LOAD_CACHE)

		locationAdapter.onClickListener = View.OnClickListener {
			val key = it.findViewById<TextView>(R.id.textview_itemmessage_subject).text.toString()
			if (key.isNotEmpty()) {
				val mapIntent = Intent(
					Intent.ACTION_VIEW, Uri.parse(
						"google.navigation:q=${keyMap[key]?.first},${keyMap[key]?.second}&mode=w"
					)
				)
				mapIntent.setPackage("com.google.android.apps.maps")
				startActivity(mapIntent)
			}
		}

		return root
	}

	private fun refreshLocations(flags: Int) {
		locationsLoading = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		locationList.clear()
		val json = JSONArray(string)
		val array = Array(json.length()) { StudyPlaceListItem(0, 0, "", "", "") }
		var currentItem: JSONObject
		var currentTitle: String

		for (i in 0 until json.length()) {
			currentItem = json.getJSONObject(i)
			currentTitle = currentItem.optString("longName").replace(", ", ",\n")
			array[currentItem.optInt("id") - 1] = StudyPlaceListItem(
				5,
				currentItem.optInt("availableSeats"),
				"0 / 100",
				currentTitle,
				currentItem.optString("openingHours")
			)
			keyMap[currentTitle] = Pair(
				currentItem.optDouble("latitude"),
				currentItem.optDouble("longitude")
			)
		}

		lateinit var loader: StringLoader
		val callback = object : StringDisplay {
			override fun onStringLoaded(string: String) {
				val innerJson = JSONArray(string)
				for (i in 0 until innerJson.length()) {
					currentItem = innerJson.getJSONObject(i)
					array[currentItem.optInt("id") - 1].let {
						it.value = currentItem.optInt("occupiedSeats")
						it.overline = resources.getString(
							R.string.study_places_occupation, it.value, it.max
						)
					}
				}
				array.sortBy { it.title }
				locationList.addAll(array)
				locationAdapter.notifyDataSetChanged()
				locationsLoading = false
				swiperefreshlayout.isRefreshing = false
			}

			override fun onStringLoadingError(code: Int) {
				when (code) {
					StringLoader.CODE_CACHE_MISSING -> loader.repeat(
						StringLoader.FLAG_LOAD_SERVER
					)
					else -> {
						MaterialAlertDialogBuilder(context)
							.setTitle(R.string.activity_title_mensa)
							.setMessage(R.string.errors_failed_loading_from_server_message)
							.setPositiveButton(R.string.all_ok) { _, _ -> }
							.show()
					}
				}
			}
		}
		loader =
			StringLoader(WeakReference(context), callback, "${API_URL}/learningPlaceOccupations")
		loader.load(StringLoader.FLAG_LOAD_CACHE)
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
				locationsLoading = false
				swiperefreshlayout.isRefreshing = false
			}
		}
	}
}
