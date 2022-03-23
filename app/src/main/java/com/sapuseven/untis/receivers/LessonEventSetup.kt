package com.sapuseven.untis.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.helpers.config.PreferenceManager
import com.sapuseven.untis.helpers.timetable.TimetableLoader
import com.sapuseven.untis.interfaces.TimetableDisplay
import com.sapuseven.untis.models.untis.UntisDate
import org.joda.time.LocalDate
import java.lang.ref.WeakReference

/**
 * A broadcast receiver template that sets up broadcasts that fire on start and end of any lesson on the current day.
 */
abstract class LessonEventSetup : BroadcastReceiver() {
	private lateinit var profileLink: LinkDatabase.Link
	private lateinit var preferenceManager: PreferenceManager

	companion object {
		const val EXTRA_LONG_PROFILE_ID = "com.sapuseven.untis.receivers.profileid"
	}

	override fun onReceive(context: Context, intent: Intent) {
		preferenceManager = PreferenceManager(context)

		loadDatabase(context, intent.getLongExtra(EXTRA_LONG_PROFILE_ID, 0))
		if (::profileLink.isInitialized) loadTimetable(context)
	}


	private fun loadDatabase(context: Context, profileId: Long) {
		val linkDatabase = LinkDatabase.createInstance(context)
		linkDatabase.getLink(profileId)?.let {
			profileLink = it
		}
	}

	private fun loadTimetable(context: Context) {
		Log.d("NotificationSetup", "loadTimetable for user ${profileLink.id}")

		val currentDate = UntisDate.fromLocalDate(LocalDate.now())

		val target = TimetableLoader.TimetableLoaderTarget(currentDate, currentDate)
		lateinit var timetableLoader: TimetableLoader
		timetableLoader = TimetableLoader(WeakReference(context), object : TimetableDisplay {
			override fun addTimetableItems(items: List<TimegridItem>, startDate: UntisDate, endDate: UntisDate, timestamp: Long) {
				onLoadingSuccess(context, items)
			}

			override fun onTimetableLoadingError(requestId: Int, code: Int?, message: String?) {
				when (code) {
					TimetableLoader.CODE_CACHE_MISSING -> timetableLoader.repeat(requestId, TimetableLoader.FLAG_LOAD_SERVER)
					else -> {
						onLoadingError(context, requestId, code, message)
					}
				}
			}
		}, profileLink)
		timetableLoader.load(target, TimetableLoader.FLAG_LOAD_CACHE)
	}

	abstract fun onLoadingSuccess(context: Context, items: List<TimegridItem>)

	abstract fun onLoadingError(context: Context, requestId: Int, code: Int?, message: String?)
}

/**
 * Creates a copy of a zipped list with the very last element duplicated into a new Pair whose second element is null.
 */
internal fun <E> List<Pair<E?, E?>>.withLast(): List<Pair<E?, E?>> =
		if (this.isEmpty()) this
		else this.toMutableList().apply { add(Pair(this.last().second, null)) }.toList()
