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
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.io.StringReader
import java.lang.ref.WeakReference
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
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/hskampus-broker/api"
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
			StringLoader(WeakReference(context), this, "${API_URL}/calendar/schedule/current")
		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = eventAdapter
		swiperefreshlayout.isRefreshing = eventsLoading
		swiperefreshlayout.setOnRefreshListener { refreshEvents(StringLoader.FLAG_LOAD_SERVER) }

		refreshEvents(StringLoader.FLAG_LOAD_CACHE)

		eventAdapter.onClickListener = View.OnClickListener {
			val title = it.findViewById<TextView>(R.id.textview_itemmessage_subject).text.toString()
			val key = title + it.findViewById<TextView>(R.id.textview_itemmessage_body)
				.text.toString()
			val intent = Intent(Intent.ACTION_INSERT)
				.setData(CalendarContract.Events.CONTENT_URI)
				.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, keyMap[key]?.first)
				.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, keyMap[key]?.second)
				.putExtra(CalendarContract.Events.TITLE, title)
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
		val treeMap = TreeMap<Long, ListItem>()
		val calendar = CalendarBuilder().build(StringReader(string)) ?: return
		val timeZone: DateTimeZone =
			DateTimeZone.forID(calendar.getProperty("X-WR-TIMEZONE").value)
		var component: Component
		var title: String
		var start: Long
		var end: Long
		var summary: String
		for (i in calendar.components) {
			component = i as Component
			title = component.getProperty("SUMMARY").value
			start = getMillis(component.getProperty("DTSTART").value, timeZone)
			end = getMillis(component.getProperty("DTEND").value, timeZone)
			summary = millisToReadableDate(start, end)
			treeMap[start] = ListItem(title, summary)
			keyMap[title + summary] = Pair(start, end)
		}
		eventList.addAll(treeMap.values)
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

	private fun getMillis(string: String, timeZone: DateTimeZone): Long {
		val dateTime = if (string.contains('T')) {
			DateTime(
				string.substring(0, 4).toInt(),
				string.substring(4, 6).toInt(),
				string.substring(6, 8).toInt(),
				string.substring(9, 11).toInt(),
				string.substring(11, 13).toInt(),
				string.substring(13, 15).toInt(),
				timeZone
			)
		} else {
			DateTime(
				string.substring(0, 4).toInt(),
				string.substring(4, 6).toInt(),
				string.substring(6, 8).toInt(),
				0,
				0,
				timeZone
			)
		}
		return dateTime.plusMillis(timeZone.getOffset(dateTime.toInstant())).millis
	}

	private fun millisToReadableDate(start: Long, end: Long): String {
		val dateFormatter = DateFormat.getMediumDateFormat(context)
		val timeFormatter = DateFormat.getTimeFormat(context)
		return dateFormatter.format(start) + ", " +
				timeFormatter.format(start) + " - " +
				dateFormatter.format(end) + ", " +
				timeFormatter.format(end)
	}
}
