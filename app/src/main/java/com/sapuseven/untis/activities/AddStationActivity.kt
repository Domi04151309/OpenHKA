package com.sapuseven.untis.activities

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.ChecklistAdapter
import com.sapuseven.untis.data.lists.ChecklistItem
import com.sapuseven.untis.helpers.StationUtils
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Integer.min
import java.lang.ref.WeakReference
import java.net.URLEncoder

class AddStationActivity : BaseActivity(), StringDisplay {
	private val list = arrayListOf<ChecklistItem>()
	private val adapter = ChecklistAdapter(list)
	private val keyMap: MutableMap<String, String> = mutableMapOf()
	private var favorites = mutableSetOf<String?>()
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
		favorites = StationUtils.getFavorites(preferences)

		recyclerview.layoutManager = LinearLayoutManager(this)
		recyclerview.adapter = adapter
		swiperefreshlayout.setOnRefreshListener { refreshStations(StringLoader.FLAG_LOAD_SERVER) }

		refreshStations(StringLoader.FLAG_LOAD_CACHE)

		adapter.onClickListener = View.OnClickListener {
			val key = it.findViewById<TextView>(R.id.textview_itemmessage_subject).text.toString() +
					it.findViewById<TextView>(R.id.textview_itemmessage_body).text.toString()
			val checked = if (favorites.contains(keyMap[key])) {
				StationUtils.removeFavorite(preferences, keyMap[key])
				favorites.remove(keyMap[key])
				false
			} else {
				StationUtils.addFavorite(preferences, keyMap[key])
				favorites.add(keyMap[key])
				true
			}
			it.findViewById<ImageView>(R.id.textview_itemmessage_check).visibility =
				if (checked) View.VISIBLE else View.GONE

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
					refreshStations(StringLoader.FLAG_LOAD_SERVER)
					return true
				}
			})
		return true
	}

	private fun refreshStations(flags: Int) {
		swiperefreshlayout.isRefreshing = true
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
		var id: String
		for (i in 0 until min(json.length(), 20)) {
			currentItem = json.optJSONObject(i)
			if (currentItem.optString("anyType") != "stop") continue
			id = currentItem.optString("stateless").run {
				val delimiter = indexOf(':')
				if (delimiter > -1) substring(0, delimiter)
				else this
			}
			list.add(
				ChecklistItem(
					currentItem.optString("object"),
					currentItem.optString("mainLoc"),
					favorites.contains(id)
				)
			)
			keyMap[currentItem.optString("object") + currentItem.optString("mainLoc")] =
				currentItem.optString("stateless").run {
					val delimiter = indexOf(':')
					if (delimiter > -1) substring(0, delimiter)
					else this
				}
		}
		adapter.notifyDataSetChanged()
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
				swiperefreshlayout.isRefreshing = false
			}
		}
	}
}
