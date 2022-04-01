package com.sapuseven.untis.activities

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.MessageAdapter
import com.sapuseven.untis.data.lists.ListItem
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import kotlinx.android.synthetic.main.activity_infocenter.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


class EventActivity : BaseActivity(), StringDisplay {
	private val eventList = arrayListOf<ListItem>()
	private val eventAdapter = MessageAdapter(eventList)
	private var eventsLoading = true
	private val keyMap: MutableMap<String, Pair<Long, Long>> = mutableMapOf()
	private lateinit var stringLoader: StringLoader

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_infocenter)

		stringLoader =
			StringLoader(WeakReference(this), this, "${API_URL}/officialcalendar/v2/current")

		recyclerview_infocenter.layoutManager = LinearLayoutManager(this)
		recyclerview_infocenter.adapter = eventAdapter
		swiperefreshlayout_infocenter.isRefreshing = eventsLoading
		swiperefreshlayout_infocenter.setOnRefreshListener { refreshEvents(StringLoader.FLAG_LOAD_SERVER) }

		refreshEvents(StringLoader.FLAG_LOAD_CACHE)

		eventAdapter.onClickListener = View.OnClickListener {
			val key = it.findViewById<TextView>(R.id.textview_itemmessage_subject).text.toString()
			val intent = Intent(Intent.ACTION_INSERT)
				.setData(CalendarContract.Events.CONTENT_URI)
				.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, keyMap[key]?.first)
				.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, keyMap[key]?.second)
				.putExtra(CalendarContract.Events.TITLE, key)
			startActivity(intent)
		}
	}

	private fun refreshEvents(flags: Int) {
		eventsLoading = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		eventList.clear()
		val json = JSONObject(string).optJSONArray("entries") ?: JSONArray()
		var currentEvent: JSONObject
		var currentTitle: String
		var currentDates: JSONObject
		for (i in 0 until json.length()) {
			currentEvent = json.getJSONObject(i)
			currentTitle = currentEvent.optString("description")
			currentDates = (currentEvent.optJSONArray("dates") ?: JSONArray()).optJSONObject(0)
				?: JSONObject()
			eventList.add(
				ListItem(
					currentTitle,
					parseDate(currentDates, "firstDate") +
							" - " +
							parseDate(currentDates, "lastDate")
				)
			)
			keyMap[currentTitle] = Pair(
				getMillis(currentDates, "firstDate"),
				getMillis(currentDates, "lastDate")
			)
		}
		eventAdapter.notifyDataSetChanged()
		eventsLoading = false
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
				eventsLoading = false
				swiperefreshlayout_infocenter.isRefreshing = false
			}
		}
	}

	private fun parseDate(json: JSONObject, key: String): String {
		return DateFormat.getMediumDateFormat(this)
			.format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(json.optString(key)) ?: Date())
			?: ""
	}

	private fun getMillis(json: JSONObject, key: String): Long {
		return SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(json.optString(key))?.time ?: 0
	}
}
