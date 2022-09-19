package com.sapuseven.untis.fragments

import android.content.Intent
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.AddStationActivity
import com.sapuseven.untis.adapters.MessageAdapter
import com.sapuseven.untis.data.lists.ListItem
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Integer.min
import java.lang.ref.WeakReference


//TODO: add offline support
class StationsFragment : Fragment(), StringDisplay {
	private val stationList = arrayListOf<ListItem>()
	private val stationAdapter = MessageAdapter(stationList)
	private var stationsLoading = true
	private val keyMap: MutableMap<String, JSONObject> = mutableMapOf()
	private var requestCounter = 0
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	//TODO: dynamic favorites
	private val favorites = arrayOf("7000037", "7001004")

	companion object {
		internal const val API_URL: String =
			"https://projekte.kvv-efa.de/sl3-alone/XSLT_DM_REQUEST?outputFormat=JSON&coordOutputFormat=WGS84[dd.ddddd]&depType=stopEvents&locationServerActive=1&mode=direct&type_dm=stop&useOnlyStops=1&useRealtime=1&name_dm="
		private const val FRAGMENT_TAG_STATION: String = "com.sapuseven.untis.fragments.station"
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

		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = stationAdapter
		swiperefreshlayout.isRefreshing = stationsLoading
		swiperefreshlayout.setOnRefreshListener { refreshStations() }

		refreshStations()

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

	private fun refreshStations() {
		stationList.clear()
		stationsLoading = true
		requestCounter = 0
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
			stationAdapter.notifyDataSetChanged()
			stationsLoading = false
			swiperefreshlayout.isRefreshing = false
		}
	}

	override fun onStringLoaded(string: String) {
		requestCounter++

		val json = JSONObject(string)
		val stop = (((json.optJSONObject("dm") ?: JSONObject()).optJSONObject("points")
			?: JSONObject()).optJSONObject("point") ?: JSONObject())
		val title = stop.optString("name")
		val departures = json.optJSONArray("departureList") ?: JSONArray()
		val parsedDepartures = Array(min(departures.length(), 10)) { "" }
		for (i in parsedDepartures.indices) {
			parsedDepartures[i] = (departures.getJSONObject(i).optJSONObject("servingLine") ?: JSONObject()).optString("number")
		}
		stationList.add(ListItem(title, parsedDepartures.joinToString(", ")))
		keyMap[title] = json

		checkRequests()
	}

	override fun onStringLoadingError(code: Int) {
		requestCounter++
		Toast.makeText(
			context,
			R.string.errors_failed_loading_from_server_message,
			Toast.LENGTH_LONG
		).show()
		checkRequests()
	}
}
