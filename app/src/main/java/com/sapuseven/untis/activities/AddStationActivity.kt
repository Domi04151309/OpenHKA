package com.sapuseven.untis.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.MessageAdapter
import com.sapuseven.untis.data.lists.ListItem
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

class AddStationActivity : BaseActivity(), StringDisplay {
	private val stationList = arrayListOf<ListItem>()
	private val stationAdapter = MessageAdapter(stationList)
	private var stationsLoading = true
	private val keyMap: MutableMap<String, String> = mutableMapOf()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val API_URL: String = "https://www.kvv.de/tunnelEfaDirect.php?outputFormat=JSON&coordOutputFormat=WGS84[dd.ddddd]&"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.fragment_infocenter)

		stringLoader = StringLoader(
			WeakReference(this),
			this,
			"${API_URL}action=XSLT_STOPFINDER_REQUEST&name_sf=Kunstakademie/Hochschule&type_sf=any"
		)
		recyclerview = findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(this)
		recyclerview.adapter = stationAdapter
		swiperefreshlayout.isRefreshing = stationsLoading
		swiperefreshlayout.setOnRefreshListener { refreshStations(StringLoader.FLAG_LOAD_SERVER) }

		refreshStations(StringLoader.FLAG_LOAD_CACHE)

		stationAdapter.onClickListener = View.OnClickListener {
			val key = it.findViewById<TextView>(R.id.textview_itemmessage_subject).text.toString()
			if (key.isNotEmpty()) {
				val mapIntent = Intent(
					Intent.ACTION_VIEW, Uri.parse(
						"geo:0,0?q=${keyMap[key]}"
					)
				)
				mapIntent.setPackage("com.google.android.apps.maps")
				startActivity(mapIntent)
			}
		}
	}

	private fun refreshStations(flags: Int) {
		stationsLoading = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		stationList.clear()
		val json = (JSONObject(string).optJSONObject("stopFinder") ?: JSONObject()).run {
			optJSONArray("points")
				?: JSONArray().put(
					(optJSONObject("points") ?: JSONObject()).optJSONObject("point") ?: JSONObject()
				)
		}
		var currentItem: JSONObject
		var refs: JSONObject
		for (i in 0 until json.length()) {
			currentItem = json.optJSONObject(i)
			refs = (currentItem.optJSONObject("ref") ?: JSONObject())
			stationList.add(
				ListItem(
					currentItem.optString("name"),
					refs.optString("id")
				)
			)
			keyMap[currentItem.optString("name")] = refs.optString("coords")
				.split(",").toMutableList().apply {
					if (size < 2) return
					val temp = this[1]
					this[1] = this[0]
					this[0] = temp
				}.joinToString(",")
		}
		stationAdapter.notifyDataSetChanged()
		stationsLoading = false
		swiperefreshlayout.isRefreshing = false
	}

	override fun onStringLoadingError(code: Int) {
		when (code) {
			StringLoader.CODE_CACHE_MISSING -> stringLoader.repeat(
				StringLoader.FLAG_LOAD_SERVER
			)
			else -> {
				Toast.makeText(
					this,
					R.string.errors_failed_loading_from_server_message,
					Toast.LENGTH_LONG
				).show()
				stationsLoading = false
				swiperefreshlayout.isRefreshing = false
			}
		}
	}
}
