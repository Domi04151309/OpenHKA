package com.sapuseven.untis.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.BaseActivity
import com.sapuseven.untis.activities.MainActivity
import com.sapuseven.untis.adapters.DepartureAdapter
import com.sapuseven.untis.data.lists.DepartureListItem
import com.sapuseven.untis.helpers.StationUtils
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat

class StationDetailsFragment(private var item: JSONObject) : Fragment() {

	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		menu.clear()
		inflater.inflate(R.menu.fragment_station_details_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		val point = (((this.item.optJSONObject("dm") ?: JSONObject()).optJSONObject("points")
			?: JSONObject()).optJSONObject("point") ?: JSONObject())
		return when (item.itemId) {
			R.id.maps -> {
				// lat and long are swapped
				val coordinates =
					(point.optJSONObject("ref") ?: JSONObject()).optString("coords")
						.split(",").toMutableList().apply {
							if (size < 2) return@apply
							val temp = this[1]
							this[1] = this[0]
							this[0] = temp
						}.joinToString(",")
				if (coordinates.isNotEmpty()) {
					val mapIntent = Intent(
						Intent.ACTION_VIEW, Uri.parse(
							"geo:0,0?q=$coordinates"
						)
					)
					mapIntent.setPackage("com.google.android.apps.maps")
					startActivity(mapIntent)
				} else {
					MaterialAlertDialogBuilder(context)
						.setTitle(R.string.all_details)
						.setMessage(R.string.errors_failed_loading_from_server_message)
						.setPositiveButton(R.string.all_ok) { _, _ -> }
						.show()
				}
				true
			}
			R.id.fav -> {
				MaterialAlertDialogBuilder(context)
					.setTitle(R.string.stations_remove_favorite)
					.setMessage(R.string.stations_remove_favorite_summary)
					.setPositiveButton(R.string.stations_remove_favorite_button) { _, _ ->
						StationUtils.removeFavorite(
							(activity as BaseActivity).preferences,
							point.optString("stateless").run {
								val delimiter = indexOf(':')
								if (delimiter > -1) substring(0, delimiter)
								else this
							})
						parentFragmentManager.popBackStackImmediate()
					}
					.setNegativeButton(R.string.all_cancel) { _, _ -> }
					.show()
				true
			}
			else -> false
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val root = inflater.inflate(
			R.layout.fragment_station_details_page,
			container,
			false
		)

		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)
		swiperefreshlayout.setOnRefreshListener {
			StringLoader(
				WeakReference(context),
				object : StringDisplay {
					override fun onStringLoaded(string: String) {
						item = JSONObject(string)
						refreshView(root)
					}

					override fun onStringLoadingError(code: Int) {
						Toast.makeText(
							context,
							R.string.errors_failed_loading_from_server_message,
							Toast.LENGTH_LONG
						).show()
						swiperefreshlayout.isRefreshing = false
					}
				}, StationsFragment.API_URL + (((item.optJSONObject("dm")
					?: JSONObject()).optJSONObject("points")
					?: JSONObject()).optJSONObject("point")
					?: JSONObject()).optString("stateless")
			).load(StringLoader.FLAG_LOAD_SERVER)
		}
		refreshView(root)

		return root
	}

	private fun refreshView(root: View) {
		root.findViewById<TextView>(R.id.name).text =
			(((item.optJSONObject("dm") ?: JSONObject()).optJSONObject("points")
				?: JSONObject()).optJSONObject("point") ?: JSONObject()).optString("name")

		root.findViewById<TextView>(R.id.last_refresh).text = resources.getString(
			R.string.main_last_refreshed,
			DateFormat.getTimeFormat(context).format(
				SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(
					((item.optJSONArray("parameters")
						?: JSONArray()).optJSONObject(4)).optString("value")
				) ?: ""
			)
		)

		val departures = item.optJSONArray("departureList") ?: JSONArray()
		val parsedDepartures = ArrayList<DepartureListItem>(departures.length())
		var currentItem: JSONObject
		var currentLine: JSONObject
		for (i in 0 until departures.length()) {
			currentItem = departures.getJSONObject(i)
			currentLine = currentItem.optJSONObject("servingLine") ?: JSONObject()
			parsedDepartures.add(
				DepartureListItem(
					currentLine.optString("direction"),
					resources.getString(
						R.string.stations_departure_summary,
						currentItem.optString("countdown"),
						currentItem.optString("platform")
					),
					currentLine.optString("number"),
					((currentLine.optJSONArray("hints") ?: JSONArray()).optJSONObject(0)
						?: JSONObject()).optString("content") == "Niederflurwagen"
				)
			)
		}

		val recyclerview = root.findViewById<RecyclerView>(R.id.recyclerview)
		recyclerview.isNestedScrollingEnabled = false
		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = DepartureAdapter(parsedDepartures)
		swiperefreshlayout.isRefreshing = false
	}

	override fun onStart() {
		super.onStart()
		if (activity is MainActivity) (activity as MainActivity).setFullscreenDialogActionBar(R.string.all_details)
	}

	override fun onStop() {
		super.onStop()
		if (activity is MainActivity) (activity as MainActivity).setDefaultActionBar(R.string.activity_title_stations)
	}
}
