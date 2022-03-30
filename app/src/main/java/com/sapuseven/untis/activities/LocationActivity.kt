package com.sapuseven.untis.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.MensaMenuAdapter
import com.sapuseven.untis.data.mensa.ListItem
import kotlinx.android.synthetic.main.activity_infocenter.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


class LocationActivity : BaseActivity() {
	private val locationList = arrayListOf<ListItem>()
	private val locationAdapter = MensaMenuAdapter(locationList)
	private var locationsLoading = true
	private val keyMap: MutableMap<String, Pair<Double, Double>> = mutableMapOf()

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_infocenter)

		refreshMessages()

		recyclerview_infocenter.layoutManager = LinearLayoutManager(this)

		locationAdapter.onClickListener = View.OnClickListener {
			val key = it.findViewById<TextView>(R.id.textview_itemmessage_subject).text.toString()
			if (key.isNotEmpty()) {
				Log.wtf("aaa", keyMap[key].toString())
				val mapIntent = Intent(
					Intent.ACTION_VIEW, Uri.parse(
						"google.navigation:q=${keyMap[key]?.first},${keyMap[key]?.second}&mode=w"
					)
				)
				mapIntent.setPackage("com.google.android.apps.maps")
				startActivity(mapIntent)
			}
		}

		showList(
			locationAdapter,
			locationsLoading,
		) { refreshMessages() }
	}

	private fun showList(
		adapter: RecyclerView.Adapter<*>,
		refreshing: Boolean,
		refreshFunction: () -> Unit
	) {
		recyclerview_infocenter.adapter = adapter
		swiperefreshlayout_infocenter.isRefreshing = refreshing
		swiperefreshlayout_infocenter.setOnRefreshListener { refreshFunction() }
	}

	private fun refreshMessages() = GlobalScope.launch(Dispatchers.Main) {
		locationsLoading = true
		loadMessages().let {
			locationList.clear()
			locationList.addAll(it)
			locationAdapter.notifyDataSetChanged()
		}
		locationsLoading = false
		swiperefreshlayout_infocenter.isRefreshing = false
	}

	private suspend fun loadMessages(): List<ListItem> {
		val list = arrayListOf<ListItem>()
		"${API_URL}/buildings/v2/all".httpGet()
			.awaitStringResult()
			.fold({ data ->
				val json = JSONArray(data)
				var departments: JSONArray
				var currentBuilding: JSONObject
				var currentDepartment: String
				for (i in 0 until json.length()) {
					currentBuilding = json.getJSONObject(i)
					departments = currentBuilding.optJSONArray("departments") ?: JSONArray()
					if (departments.length() > 0) list.add(
						ListItem(
							"",
							currentBuilding.optString("name")
						)
					)
					for (j in 0 until departments.length()) {
						currentDepartment = departments.getJSONObject(j).optString("name")
						list.add(
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
			}, {
				//TODO: handle error
			})
		return list
	}
}
