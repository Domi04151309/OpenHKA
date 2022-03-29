package com.sapuseven.untis.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.MainActivity
import com.sapuseven.untis.helpers.config.PreferenceManager
import com.sapuseven.untis.helpers.config.PreferenceUtils
import org.joda.time.LocalDateTime

class NotificationReceiver : BroadcastReceiver() {
	companion object {
		const val CHANNEL_ID_BREAKINFO = "notifications.breakinfo"

		const val EXTRA_BOOLEAN_CLEAR = "com.sapuseven.untis.notifications.clear"
		const val EXTRA_BOOLEAN_FIRST = "com.sapuseven.untis.notifications.first"
		const val EXTRA_INT_ID = "com.sapuseven.untis.notifications.id"
		const val EXTRA_INT_BREAK_END_TIME = "com.sapuseven.untis.notifications.breakEndTimeSeconds"
		const val EXTRA_STRING_BREAK_END_TIME = "com.sapuseven.untis.notifications.breakEndTime"
		const val EXTRA_STRING_NEXT_SUBJECT = "com.sapuseven.untis.notifications.nextSubject"
		const val EXTRA_STRING_NEXT_ROOM = "com.sapuseven.untis.notifications.nextRoom"
	}

	override fun onReceive(context: Context, intent: Intent) {
		Log.d("NotificationReceiver", "NotificationReceiver received")

		val preferenceManager = PreferenceManager(context)
		if (!PreferenceUtils.getPrefBool(
				preferenceManager,
				"preference_notifications_enable"
			)
		) return

		if (intent.hasExtra(EXTRA_STRING_BREAK_END_TIME)) {
			if (LocalDateTime.now().millisOfDay >= intent.getIntExtra(
					EXTRA_INT_BREAK_END_TIME,
					0
				)
			) return // Notification delayed for too long

			createNotificationChannel(context)

			val pendingIntent =
				PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), 0)

			val title = context.getString(
				if (intent.getBooleanExtra(
						EXTRA_BOOLEAN_FIRST,
						false
					)
				) R.string.notifications_text_first_title else R.string.notifications_text_title,
				intent.getStringExtra(EXTRA_STRING_BREAK_END_TIME)
			)
			val message = buildMessage(
				null,
				intent,
				preferenceManager,
				context.getString(R.string.notifications_text_message_separator)
			)
			val longMessage = buildMessage(context, intent, preferenceManager, "\n")

			val builder = NotificationCompat.Builder(context, CHANNEL_ID_BREAKINFO)
				.setContentTitle(title)
				.setContentText(message)
				.setSmallIcon(R.drawable.notification_clock)
				.setContentIntent(pendingIntent)
				.setStyle(NotificationCompat.BigTextStyle().bigText(longMessage))
				.setAutoCancel(false)
				.setOngoing(true)
				.setCategory(NotificationCompat.CATEGORY_STATUS)

			with(NotificationManagerCompat.from(context)) {
				notify(intent.getIntExtra(EXTRA_INT_ID, -1), builder.build())
			}
			Log.d("NotificationReceiver", "notification delivered: $title")
		} else {
			Log.d(
				"NotificationReceiver",
				"Attempting to cancel notification #${intent.getIntExtra(EXTRA_INT_ID, -1)}"
			)
			with(NotificationManagerCompat.from(context)) {
				cancel(intent.getIntExtra(EXTRA_INT_ID, -1))
			}
		}
	}

	private fun buildMessage(
		context: Context?,
		intent: Intent,
		preferenceManager: PreferenceManager,
		separator: String
	) = listOfNotNull(
		if (intent.getStringExtra(EXTRA_STRING_NEXT_SUBJECT)?.isBlank() != false) null else {
			if (PreferenceUtils.getPrefBool(
					preferenceManager,
					"preference_notifications_subjects"
				)
			) context?.getString(
				R.string.notifications_text_message_subjects,
				intent.getStringExtra(EXTRA_STRING_NEXT_SUBJECT)
			)
			else null
		},
		if (intent.getStringExtra(EXTRA_STRING_NEXT_ROOM)?.isBlank() != false) null else{
			if (PreferenceUtils.getPrefBool(
					preferenceManager,
					"preference_notifications_rooms"
				)
			) context?.getString(
				R.string.notifications_text_message_rooms,
				intent.getStringExtra(EXTRA_STRING_NEXT_ROOM)
			)
			else null
		}
	).joinToString(separator)

	private fun createNotificationChannel(context: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val name = context.getString(R.string.notifications_channel_breakinfo)
			val descriptionText = context.getString(R.string.notifications_channel_breakinfo_desc)
			val importance = NotificationManager.IMPORTANCE_LOW
			val channel = NotificationChannel(CHANNEL_ID_BREAKINFO, name, importance).apply {
				description = descriptionText
			}
			val notificationManager: NotificationManager =
				context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}
}
