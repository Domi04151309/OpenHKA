package com.sapuseven.untis.fragments

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.AddStationActivity
import com.sapuseven.untis.activities.BaseActivity
import com.sapuseven.untis.adapters.MessageAdapter
import com.sapuseven.untis.adapters.StationAdapter
import com.sapuseven.untis.data.lists.DepartureListItem
import com.sapuseven.untis.data.lists.ListItem
import com.sapuseven.untis.data.lists.StationItem
import com.sapuseven.untis.helpers.StationUtils
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Integer.min
import java.lang.ref.WeakReference


class StationsFragment : Fragment(), StringDisplay {
	private val stationList = arrayListOf<StationItem>()
	private val stationAdapter = StationAdapter(stationList)
	private val keyMap: MutableMap<String, JSONObject> = mutableMapOf()
	private var fromCache: Boolean = false
	private var favorites = setOf<String?>()
	private var requestCounter = 0
	private lateinit var lastRefreshed: TextView
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		internal const val API_URL: String =
			"https://kvv.de/tunnelEfaDirect.php?outputFormat=JSON&coordOutputFormat=WGS84[dd.ddddd]&action=XSLT_DM_REQUEST&mode=direct&type_dm=stop&useRealtime=1&name_dm="
		private const val FRAGMENT_TAG_STATION: String = "com.sapuseven.untis.fragments.station"

		fun parseStation(resources: Resources, input: String): Pair<StationItem, JSONObject> {
			val json = JSONObject(input)
			val stop = (((json.optJSONObject("dm") ?: JSONObject()).optJSONObject("points")
				?: JSONObject()).optJSONObject("point") ?: JSONObject())
			val title = stop.optString("name")
			val departures = json.optJSONArray("departureList") ?: JSONArray()
			val parsedDepartures =
				Array(min(departures.length(), 5)) { DepartureListItem("") }
			var currentItem: JSONObject
			var currentLine: JSONObject
			for (i in parsedDepartures.indices) {
				currentItem = departures.getJSONObject(i)
				currentLine = currentItem.optJSONObject("servingLine") ?: JSONObject()
				parsedDepartures[i] = DepartureListItem(
					resources.getString(
						R.string.stations_departure_short_summary,
						currentLine.optString("direction"),
						currentItem.optString("countdown")
					),
					line = currentLine.optString("number")
				)
			}
			return Pair(StationItem(title, parsedDepartures), json)
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(
			R.layout.fragment_stations,
			container,
			false
		)

		lastRefreshed = root.findViewById(R.id.textview_stations_lastrefresh)
		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = stationAdapter
		swiperefreshlayout.setOnRefreshListener { refreshStations() }

		stationAdapter.onClickListener = View.OnClickListener {
			val fragment = StationDetailsFragment(
				keyMap[it.findViewById<TextView>(R.id.textview_itemmessage_subject).text]
					?: return@OnClickListener
			)
			(activity as AppCompatActivity).supportFragmentManager.beginTransaction().run {
				setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				add(R.id.content_main, fragment, FRAGMENT_TAG_STATION)
				addToBackStack(fragment.tag)
				commit()
			}
		}

		root.findViewById<FloatingActionButton>(R.id.button_stations_add).setOnClickListener {
			startActivity(Intent(context, AddStationActivity::class.java))
		}

		return root
	}

	override fun onStart() {
		super.onStart()
		refreshStations()
	}

	private fun refreshStations() {
		stationList.clear()
		swiperefreshlayout.isRefreshing = true
		requestCounter = 0
		fromCache = false
		favorites = StationUtils.getFavorites((activity as BaseActivity).preferences)
		for (station in favorites) {
			StringLoader(
				WeakReference(context),
				this,
				API_URL + station
			).load(StringLoader.FLAG_LOAD_SERVER)
		}
	}

	private fun checkRequests() {
		if (requestCounter == favorites.size) {
			if (fromCache) stationList.forEach {
				it.departures = arrayOf(
					DepartureListItem(
						resources.getString(R.string.errors_failed_loading_from_server_message)
					)
				)
			}
			stationList.sortBy { it.title }
			stationAdapter.notifyDataSetChanged()
			lastRefreshed.text = resources.getString(
				R.string.main_last_refreshed,
				DateFormat.getTimeFormat(context).format(System.currentTimeMillis())
			)
			swiperefreshlayout.isRefreshing = false
		}
	}

	override fun onStringLoaded(string: String) {
		requestCounter++
		parseStation(resources, string).run {
			stationList.add(first)
			keyMap[first.title] = second
		}
		checkRequests()
	}

	override fun onStringLoadingError(code: Int, loader: StringLoader) {
		if (code == StringLoader.CODE_REQUEST_FAILED) {
			fromCache = true
			loader.repeat(StringLoader.FLAG_LOAD_CACHE)
		} else {
			requestCounter++
			checkRequests()
		}
	}
}
