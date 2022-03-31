package com.sapuseven.untis.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.MensaMenuAdapter
import com.sapuseven.untis.data.lists.ListItem
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import kotlinx.android.synthetic.main.activity_infocenter.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference


class LocationActivity : BaseActivity(), StringDisplay {
	private val locationList = arrayListOf<ListItem>()
	private val locationAdapter = MensaMenuAdapter(locationList)
	private var locationsLoading = true
	private val keyMap: MutableMap<String, Pair<Double, Double>> = mutableMapOf()
	private lateinit var stringLoader: StringLoader

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_infocenter)

		stringLoader = StringLoader(WeakReference(this), this, "${API_URL}/buildings/v2/all")

		recyclerview_infocenter.layoutManager = LinearLayoutManager(this)
		recyclerview_infocenter.adapter = locationAdapter
		swiperefreshlayout_infocenter.isRefreshing = locationsLoading
		swiperefreshlayout_infocenter.setOnRefreshListener { refreshMessages(StringLoader.FLAG_LOAD_SERVER) }

		refreshMessages(StringLoader.FLAG_LOAD_CACHE)

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
	}

	private fun refreshMessages(flags: Int) {
		locationsLoading = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		locationList.clear()
		val json = JSONArray(string)
		var departments: JSONArray
		var currentBuilding: JSONObject
		var currentDepartment: String
		for (i in 0 until json.length()) {
			currentBuilding = json.getJSONObject(i)
			departments = currentBuilding.optJSONArray("departments") ?: JSONArray()
			if (departments.length() > 0) locationList.add(
				ListItem(
					"",
					currentBuilding.optString("name")
				)
			)
			for (j in 0 until departments.length()) {
				currentDepartment = departments.getJSONObject(j).optString("name")
				locationList.add(
					ListItem(
						currentDepartment,
						""
					)
				)
				keyMap[currentDepartment] = Pair(
					currentBuilding.optDouble("latitude"),
					currentBuilding.optDouble("longitude")
				)
			}
		}
		locationAdapter.notifyDataSetChanged()
		locationsLoading = false
		swiperefreshlayout_infocenter.isRefreshing = false
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
				locationsLoading = false
				swiperefreshlayout_infocenter.isRefreshing = false
			}
		}
	}
}
