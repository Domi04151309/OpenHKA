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
import com.sapuseven.untis.adapters.infocenter.RSSAdapter
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.data.lists.MensaPricing
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.fragments.InfoCenterFragment
import com.sapuseven.untis.fragments.MensaFragment
import com.sapuseven.untis.helpers.config.PreferenceManager
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*


class WidgetRemoteViewsFactory(private val applicationContext: Context, intent: Intent) :
	RemoteViewsFactory {
	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"

		const val EXTRA_INT_WIDGET_ID: String = "com.sapuseven.widgets.id"
		const val EXTRA_INT_WIDGET_TYPE: String = "com.sapuseven.widgets.type"

		const val WIDGET_TYPE_UNKNOWN: Int = 0
		const val WIDGET_TYPE_MESSAGES: Int = 1
		const val WIDGET_TYPE_TIMETABLE: Int = 2
		const val WIDGET_TYPE_MENSA: Int = 3

		const val STATUS_UNKNOWN: Int = 0
		const val STATUS_DONE: Int = 1
		const val STATUS_LOADING: Int = 2
		const val STATUS_ERROR: Int = 3
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
				WIDGET_TYPE_MENSA -> loadMeals()
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
				InfoCenterFragment.loadMessages(applicationContext, link ?: return@runBlocking)
					?.map { it ->
						WidgetListItem(
							0,
							it.title ?: "",
							RSSAdapter.parseDate(applicationContext, it.pubDate ?: "")
						)
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
	private fun loadTimetable(): List<WidgetListItem> {
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
							timegridItem.title.toString(),
							"${timegridItem.startTime.toString(timeFormatter)} - ${
								timegridItem.endTime.toString(
									timeFormatter
								)
							} | " + arrayOf(
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

	private fun loadMeals(): List<WidgetListItem> {
		val preferences = PreferenceManager(applicationContext, link?.id ?: -1)
		val currentID = preferences.defaultPrefs.getInt(
			MensaFragment.PREFERENCE_MENSA_ID,
			MensaFragment.DEFAULT_ID
		)
		val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
		var items: MutableList<WidgetListItem> = mutableListOf()
		runBlocking {
			"$API_URL/canteen/v2/$currentID/$date"
				.httpGet()
				.awaitStringResult()
				.fold({ data ->
					val formatter = DecimalFormat(
						"0.00", DecimalFormatSymbols.getInstance(Locale.getDefault())
					)
					val json = JSONObject(data)
					val mealGroups = json.optJSONArray("mealGroups") ?: JSONArray()
					var meals: JSONArray
					var currentGroup: JSONObject
					var currentMeal: JSONObject
					for (i in 0 until mealGroups.length()) {
						currentGroup = mealGroups.getJSONObject(i)
						items.add(WidgetListItem(0, "", currentGroup.optString("title")))
						meals = currentGroup.optJSONArray("meals") ?: JSONArray()
						for (j in 0 until meals.length()) {
							currentMeal = meals.getJSONObject(j)
							items.add(
								WidgetListItem(
									0,
									currentMeal.optString("name"),
									applicationContext.resources.getString(
										R.string.mensa_meal_price, formatter.format(
											MensaPricing(
												currentMeal.optDouble("priceStudent"),
												currentMeal.optDouble("priceGuest"),
												currentMeal.optDouble("priceEmployee"),
												currentMeal.optDouble("pricePupil")
											).getPriceFromLevel(
												applicationContext.resources, preferences.defaultPrefs
													.getString(
														MensaFragment.PREFERENCE_MENSA_PRICING_LEVEL,
														MensaFragment.DEFAULT_PRICING_LEVEL
													) ?: ""
											)
										)
									)
								)
							)
						}
					}
					status = STATUS_DONE
				}, {
					status = STATUS_ERROR
					items = mutableListOf(errorItem)
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
			items?.get(position)?.let { (_, firstLine, secondLine) ->
				setTextViewText(R.id.textview_listitem_line1, firstLine)
				setTextViewText(
					R.id.textview_listitem_line2,
					HtmlCompat.fromHtml(secondLine, HtmlCompat.FROM_HTML_MODE_COMPACT)
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
