package com.sapuseven.untis.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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
import java.text.SimpleDateFormat
import java.util.*


class EventFragment : Fragment(), StringDisplay {
	private val eventList = arrayListOf<ListItem>()
	private val eventAdapter = MessageAdapter(eventList)
	private var eventsLoading = true
	private val keyMap: MutableMap<String, Pair<Long, Long>> = mutableMapOf()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(
			R.layout.fragment_infocenter,
			container,
			false
		)

		stringLoader =
			StringLoader(WeakReference(context), this, "${API_URL}/officialcalendar/v2/current")
		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = eventAdapter
		swiperefreshlayout.isRefreshing = eventsLoading
		swiperefreshlayout.setOnRefreshListener { refreshEvents(StringLoader.FLAG_LOAD_SERVER) }

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

		return root
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
		swiperefreshlayout.isRefreshing = false
	}

	override fun onStringLoadingError(code: Int) {
		when (code) {
			StringLoader.CODE_CACHE_MISSING -> stringLoader.repeat(
				StringLoader.FLAG_LOAD_SERVER
			)
			else -> {
				Toast.makeText(
					context,
					R.string.errors_failed_loading_from_server_message,
					Toast.LENGTH_LONG
				).show()
				eventsLoading = false
				swiperefreshlayout.isRefreshing = false
			}
		}
	}

	private fun parseDate(json: JSONObject, key: String): String {
		return DateFormat.getMediumDateFormat(context)
			.format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(json.optString(key)) ?: Date())
			?: ""
	}

	private fun getMillis(json: JSONObject, key: String): Long {
		return SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(json.optString(key))?.time ?: 0
	}
}
