package com.sapuseven.untis.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.core.text.HtmlCompat
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.InfoCenterActivity
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.models.untis.timetable.Period
import com.sapuseven.untis.widgets.BaseWidget.Companion.EXTRA_INT_RELOAD
import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.io.StringReader


class WidgetRemoteViewsFactory(private val applicationContext: Context, intent: Intent) :
	RemoteViewsFactory {
	companion object {
		const val EXTRA_INT_WIDGET_ID = "com.sapuseven.widgets.id"
		const val EXTRA_INT_WIDGET_TYPE = "com.sapuseven.widgets.type"

		const val WIDGET_TYPE_UNKNOWN = 0
		const val WIDGET_TYPE_MESSAGES = 1
		const val WIDGET_TYPE_TIMETABLE = 2

		const val STATUS_UNKNOWN = 0
		const val STATUS_DONE = 1
		const val STATUS_LOADING = 2
		const val STATUS_ERROR = 3
	}

	private val appWidgetId =
		intent.getIntExtra(EXTRA_INT_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
	private val linkDatabase = LinkDatabase.createInstance(applicationContext)
	private val link = linkDatabase.getLink(loadIdPref(applicationContext, appWidgetId))
	private val type = intent.getIntExtra(EXTRA_INT_WIDGET_TYPE, WIDGET_TYPE_UNKNOWN)
	private var items: List<WidgetListItem>? = null

	private var status = STATUS_UNKNOWN

	private val errorItem =
		WidgetListItem(0, "Failed to load data", "Tap to retry") // TODO: Extract string resources

	private fun loadItems() {
		status = STATUS_LOADING
		try {
			items = when (type) {
				WIDGET_TYPE_MESSAGES -> loadMessages()
				WIDGET_TYPE_TIMETABLE -> loadTimetable()
				else -> emptyList()
			}
		} catch (e: Exception) {
			// TODO: Implement proper error handling
		}
	}

	private fun loadMessages(): List<WidgetListItem>? {
		var items: List<WidgetListItem>? = emptyList()
		var success = false
		runBlocking {
			items =
				InfoCenterActivity.loadMessages(applicationContext, link ?: return@runBlocking)
					?.map { it ->
						WidgetListItem(0, it.title ?: "", it.pubDate ?: "")
					}
			success = true
		}
		return if (success) {
			status = STATUS_DONE
			items
		} else {
			status = STATUS_ERROR
			listOf(errorItem)
		}
	}

	// TODO: This function duplicates code from TimetableLoader. This should be resolved during backend refactoring.
	private fun loadTimetable(): List<WidgetListItem>? {
		if (link == null) {
			status = STATUS_ERROR
			listOf(errorItem)
		}
		var items: List<WidgetListItem> = emptyList()
		runBlocking {
			val timeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("HH:mm")
			link!!.iCalUrl
				.httpGet()
				.awaitStringResult()
				.fold({ data ->
					val calendar = CalendarBuilder().build(StringReader(data))
					val timeZone: DateTimeZone =
						DateTimeZone.forID(calendar.getProperty("X-WR-TIMEZONE").value)
					val timestamp = DateTime(Instant.now().millis)
					items = calendar.components.filter {
						val start = Period.stringToDate(
							(it as Component).getProperty("DTSTART").value,
							timeZone
						)
						start.dayOfYear() == timestamp.dayOfYear() && start.year() == timestamp.year()
					}.map {
						val timegridItem = TimegridItem(Period(it as Component, timeZone))
						WidgetListItem(
							timegridItem.id,
							"${timegridItem.startTime.toString(timeFormatter)} - ${
								timegridItem.endTime.toString(
									timeFormatter
								)
							} | ${timegridItem.title}",
							arrayOf(
								timegridItem.top,
								timegridItem.bottom
							).filter { s -> s.isNotBlank() }
								.joinToString()
						)
					}
					status = STATUS_DONE
				}, {
					status = STATUS_ERROR
					items = listOf(errorItem)
				})
		}
		return items
	}

	override fun onCreate() {
		Log.d("Widgets", "onCreate() for widget #${appWidgetId}")
		loadItems()
	}

	override fun onDataSetChanged() {
		Log.d("Widgets", "onDataSetChanged() for widget #${appWidgetId}")
		loadItems()
	}

	override fun onDestroy() {}

	override fun getViewAt(position: Int): RemoteViews {
		return RemoteViews(applicationContext.packageName, R.layout.widget_base_item).apply {
			items?.get(position)?.let { item: WidgetListItem ->
				setTextViewText(R.id.textview_listitem_line1, item.firstLine)
				setTextViewText(
					R.id.textview_listitem_line2,
					HtmlCompat.fromHtml(item.secondLine, HtmlCompat.FROM_HTML_MODE_COMPACT)
				)
			}

			val reloadIntent = Intent()
				.putExtra(EXTRA_INT_RELOAD, status == STATUS_ERROR)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
			setOnClickFillInIntent(R.id.linearlayout_widget_listitem_root, reloadIntent)
		}
	}

	override fun getCount(): Int = items?.size ?: 0

	override fun getLoadingView(): RemoteViews? = null

	override fun getViewTypeCount(): Int = 1

	override fun getItemId(position: Int): Long = items?.get(position)?.id ?: position.toLong()

	override fun hasStableIds(): Boolean = true

	data class WidgetListItem(
		val id: Long,
		val firstLine: String,
		val secondLine: String
	)
}

class WidgetRemoteViewsService : RemoteViewsService() {
	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		return WidgetRemoteViewsFactory(this.applicationContext, intent)
	}
}
