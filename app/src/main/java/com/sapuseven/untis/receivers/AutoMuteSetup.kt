package com.sapuseven.untis.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.util.Log
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.helpers.config.PreferenceManager
import com.sapuseven.untis.helpers.config.PreferenceUtils
import com.sapuseven.untis.models.untis.timetable.Period
import com.sapuseven.untis.receivers.AutoMuteReceiver.Companion.EXTRA_BOOLEAN_MUTE
import com.sapuseven.untis.receivers.AutoMuteReceiver.Companion.EXTRA_INT_ID
import org.joda.time.LocalDateTime

class AutoMuteSetup : LessonEventSetup() {
	private lateinit var preferenceManager: PreferenceManager

	override fun onReceive(context: Context, intent: Intent) {
		Log.d("AutoMuteSetup", "AutoMuteSetup received")

		preferenceManager = PreferenceManager(context)
		if (PreferenceUtils.getPrefBool(preferenceManager, "preference_automute_enable"))
			super.onReceive(context, intent)
	}

	override fun onLoadingSuccess(context: Context, items: List<TimegridItem>) {
		items.sortedBy { it.startTime }.zipWithNext().withLast().forEach {
			it.first?.let { item ->
				val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
				val id = item.startTime.millisOfDay / 1000

				if (item.endTime.millisOfDay <= LocalDateTime.now().millisOfDay) return@forEach
				if (item.period.type == Period.Type.CANCELLED && !PreferenceUtils.getPrefBool(preferenceManager, "preference_automute_cancelled_lessons")) return@forEach

				val muteIntent = Intent(context, AutoMuteReceiver::class.java)
						.putExtra(EXTRA_INT_ID, id)
						.putExtra(EXTRA_BOOLEAN_MUTE, true)
				val pendingMuteIntent = PendingIntent.getBroadcast(context, item.startTime.millisOfDay, muteIntent, 0)
				alarmManager.setExact(AlarmManager.RTC_WAKEUP, item.startTime.millis, pendingMuteIntent)
				Log.d("AutoMuteSetup", "${item.period.title} mute scheduled for ${item.startTime}")

				val minimumBreakLengthMillis = PreferenceUtils.getPrefInt(preferenceManager, "preference_automute_minimum_break_length") * 60 * 1000
				if (it.second != null
						&& it.second!!.startTime.millisOfDay - item.endTime.millisOfDay < minimumBreakLengthMillis)
							return@forEach // No break exists or break it's short, don't unmute
				val unmuteIntent = Intent(context, AutoMuteReceiver::class.java)
						.putExtra(EXTRA_INT_ID, id)
						.putExtra(EXTRA_BOOLEAN_MUTE, false)
				val pendingUnmuteIntent = PendingIntent.getBroadcast(context, item.endTime.millisOfDay, unmuteIntent, 0)
				alarmManager.setExact(AlarmManager.RTC_WAKEUP, item.endTime.millis, pendingUnmuteIntent)
				Log.d("AutoMuteSetup", "${item.period.title} unmute scheduled for ${item.endTime}")
			}
		}
	}

	override fun onLoadingError(context: Context, requestId: Int, code: Int?, message: String?) {}
}
