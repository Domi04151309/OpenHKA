package com.sapuseven.untis.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
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
import java.lang.Integer.min
import java.lang.ref.WeakReference
import java.net.URLEncoder

class AddStationActivity : BaseActivity(), StringDisplay {
	private val list = arrayListOf<ListItem>()
	private val adapter = MessageAdapter(list)
	private var stationsLoading = true
	private val keyMap: MutableMap<String, String> = mutableMapOf()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		// The 0 is there to prevent auto stop selection by the API
		private const val API_URL: String =
			"https://kvv.de/tunnelEfaDirect.php?outputFormat=JSON&coordOutputFormat=WGS84[dd.ddddd]&action=XSLT_STOPFINDER_REQUEST&type_sf=any&name_sf=0"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.fragment_infocenter)

		stringLoader = StringLoader(
			WeakReference(this),
			this,
			API_URL + "Kunstakademie/Hochschule"
		)
		recyclerview = findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(this)
		recyclerview.adapter = adapter
		swiperefreshlayout.isRefreshing = stationsLoading
		swiperefreshlayout.setOnRefreshListener { refreshStations(StringLoader.FLAG_LOAD_SERVER) }

		refreshStations(StringLoader.FLAG_LOAD_CACHE)

		adapter.onClickListener = View.OnClickListener {
			val key = it.findViewById<TextView>(R.id.textview_itemmessage_subject).text.toString() +
					it.findViewById<TextView>(R.id.textview_itemmessage_body).text.toString()
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

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_people_menu, menu)
		(menu.findItem(R.id.search).actionView as SearchView)
			.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
				override fun onQueryTextSubmit(query: String): Boolean {
					return onQueryTextChange(query)
				}

				override fun onQueryTextChange(newText: String): Boolean {
					if (newText.trim().isEmpty()) return true
					stringLoader = StringLoader(
						WeakReference(this@AddStationActivity),
						this@AddStationActivity,
						API_URL + URLEncoder.encode(newText, "UTF-8")
					)
					stationsLoading = true
					swiperefreshlayout.isRefreshing = true
					refreshStations(StringLoader.FLAG_LOAD_SERVER)
					return true
				}
			})
		return true
	}

	private fun refreshStations(flags: Int) {
		stationsLoading = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		list.clear()
		val json = (JSONObject(string).optJSONObject("stopFinder") ?: JSONObject()).run {
			optJSONArray("points")
				?: JSONArray().put(
					(optJSONObject("points") ?: JSONObject()).optJSONObject("point") ?: JSONObject()
				)
		}
		var currentItem: JSONObject
		var refs: JSONObject
		for (i in 0 until min(json.length(), 20)) {
			currentItem = json.optJSONObject(i)
			if (currentItem.optString("anyType") != "stop") continue

			refs = (currentItem.optJSONObject("ref") ?: JSONObject())
			list.add(
				ListItem(
					currentItem.optString("object"),
					currentItem.optString("mainLoc")
				)
			)
			keyMap[currentItem.optString("object") + currentItem.optString("mainLoc")] =
				refs.optString("coords").split(",").toMutableList().apply {
					if (size < 2) return@apply
					val temp = this[1]
					this[1] = this[0]
					this[0] = temp
				}.joinToString(",")
		}
		adapter.notifyDataSetChanged()
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
