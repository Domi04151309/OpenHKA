package com.sapuseven.untis.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.MainActivity
import com.sapuseven.untis.adapters.DepartureAdapter
import com.sapuseven.untis.data.lists.DepartureListItem
import org.json.JSONArray
import org.json.JSONObject

class StationDetailsFragment(private val item: JSONObject) : Fragment() {

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
		return if (item.itemId == R.id.maps) {
			val coordinates =
				((((this.item.optJSONObject("dm") ?: JSONObject()).optJSONObject("points")
					?: JSONObject()).optJSONObject("point") ?: JSONObject()).optJSONObject("ref")
					?: JSONObject()).optString("coords").split(",").toMutableList().apply {
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
		} else false
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

		root.findViewById<TextView>(R.id.name).text =
			(((item.optJSONObject("dm") ?: JSONObject()).optJSONObject("points")
				?: JSONObject()).optJSONObject("point") ?: JSONObject()).optString("name")


		val departures = item.optJSONArray("departureList") ?: JSONArray()
		val parsedDepartures = ArrayList<DepartureListItem>(departures.length())
		var currentItem: JSONObject
		var currentLine: JSONObject
		for (i in 0 until departures.length()) {
			currentItem = departures.getJSONObject(i)
			currentLine = (currentItem.optJSONObject("servingLine") ?: JSONObject())
			parsedDepartures.add(
				DepartureListItem(
					currentLine.optString("direction"),
					resources.getString(
						R.string.stations_departure_summary,
						currentItem.optString("countdown"),
						currentItem.optString("platform")
					),
					currentLine.optString("number")
				)
			)
		}

		val recyclerview = root.findViewById<RecyclerView>(R.id.recyclerview)
		recyclerview.isNestedScrollingEnabled = false
		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = DepartureAdapter(parsedDepartures)

		return root
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