package com.sapuseven.untis.helpers.timetable

import android.content.Context
import android.util.Log
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.interfaces.TimetableDisplay
import com.sapuseven.untis.models.untis.UntisDate
import com.sapuseven.untis.models.untis.timetable.Period
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.ComponentList
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import java.io.StringReader
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList


class TimetableLoader(
	private val context: WeakReference<Context>,
	private val timetableDisplay: TimetableDisplay,
	private val link: LinkDatabase.Link,
) {
	companion object {
		const val FLAG_LOAD_CACHE = 0b00000001
		const val FLAG_LOAD_SERVER = 0b00000010

		const val CODE_CACHE_MISSING = 1
		const val CODE_REQUEST_FAILED = 2
		const val CODE_REQUEST_PARSING_EXCEPTION = 3
	}

	private val requestList = ArrayList<TimetableLoaderTarget>()

	private var ids: Long = 0

	fun load(target: TimetableLoaderTarget, flags: Int = 0) =
		GlobalScope.launch(Dispatchers.Main) {
			requestList.add(target)

			if (flags and FLAG_LOAD_CACHE > 0)
				loadFromCache(target, requestList.size - 1)
			if (flags and FLAG_LOAD_SERVER > 0)
				loadFromServer(target, requestList.size - 1)
		}

	private fun loadFromCache(target: TimetableLoaderTarget, requestId: Int) {
		val cache = TimetableCache(context)
		cache.setTarget(target.startDate, target.endDate, target.id)

		if (cache.exists()) {
			Log.d(
				"TimetableLoaderDebug",
				"target $target (requestId $requestId): cached file found"
			)
			cache.load()?.let { cacheObject ->
				val calendar = parseICal(cacheObject.data)
				val timeZone: DateTimeZone =
					DateTimeZone.forID(calendar.getProperty("X-WR-TIMEZONE").value)
				timetableDisplay.addTimetableItems(calendar.components.map {
					TimegridItem(
						ids,
						Period(it as Component, timeZone)
					)
				}, target.startDate, target.endDate, cacheObject.timestamp)
			} ?: run {
				cache.delete()
				Log.d(
					"TimetableLoaderDebug",
					"target $target (requestId $requestId): cached file corrupted"
				)
				timetableDisplay.onTimetableLoadingError(
					requestId,
					CODE_CACHE_MISSING,
					"cached timetable corrupted"
				)
			}
		} else {
			Log.d(
				"TimetableLoaderDebug",
				"target $target (requestId $requestId): cached file missing"
			)
			timetableDisplay.onTimetableLoadingError(
				requestId,
				CODE_CACHE_MISSING,
				"no cached timetable found"
			)
		}
	}

	private suspend fun loadFromServer(
		target: TimetableLoaderTarget,
		requestId: Int
	) {
		val cache = TimetableCache(context)
		cache.setTarget(target.startDate, target.endDate, target.id)

		link.iCalUrl
			.httpGet()
			.awaitStringResult()
			.fold({ data ->
				val calendar = parseICal(data)
				val timeZone: DateTimeZone =
					DateTimeZone.forID(calendar.getProperty("X-WR-TIMEZONE").value)
				val timestamp = Instant.now().millis
				timetableDisplay.addTimetableItems(calendar.components.map {
					TimegridItem(
						ids,
						Period(it as Component, timeZone)
					)
				}, target.startDate, target.endDate, timestamp)
				Log.d(
					"TimetableLoaderDebug",
					"target $target (requestId $requestId): saving to cache: $cache"
				)
				cache.save(TimetableCache.CacheObject(timestamp, data))
			}, {
				//TODO: handle error
			})
	}

	private fun parseICal(data: String): Calendar {
		return CalendarBuilder().build(StringReader(data))!!
	}

	fun repeat(requestId: Int, flags: Int = 0) {
		Log.d(
			"TimetableLoaderDebug",
			"target ${requestList[requestId]} (requestId $requestId): repeat"
		)
		load(requestList[requestId], flags)
	}

	data class TimetableLoaderTarget(
		val startDate: UntisDate,
		val endDate: UntisDate,
		val id: Int
	)
}
