package com.sapuseven.untis.activities

import android.os.Bundle
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
import java.text.SimpleDateFormat
import java.util.*

class LocationActivity : BaseActivity() {
	private val locationList = arrayListOf<ListItem>()
	private val locationAdapter = MensaMenuAdapter(locationList)
	private var locationsLoading = true

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_infocenter)

		refreshMessages()

		recyclerview_infocenter.layoutManager = LinearLayoutManager(this)

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

			preferences.defaultPrefs.edit()
				.putInt("preference_last_messages_count", it.size)
				.putString(
					"preference_last_messages_date",
					SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Calendar.getInstance().time)
				)
				.apply()
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
				var currentDepartment: JSONObject
				for (i in 0 until json.length()) {
					currentBuilding = json.getJSONObject(i)
					list.add(ListItem("", currentBuilding.optString("name")))
					departments = currentBuilding.optJSONArray("departments") ?: JSONArray()
					for (j in 0 until departments.length()) {
						currentDepartment = departments.getJSONObject(j)
						list.add(
							ListItem(
								currentDepartment.optString("name"),
								currentDepartment.optString("shortName")
							)
						)
					}
				}
			}, {
				//TODO: handle error
			})
		return list
	}
}
