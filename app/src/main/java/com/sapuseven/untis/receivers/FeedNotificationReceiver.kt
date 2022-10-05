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
import androidx.core.text.HtmlCompat
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.MainActivity
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.fragments.InfoCenterFragment
import com.sapuseven.untis.helpers.config.PreferenceManager
import com.sapuseven.untis.helpers.config.PreferenceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Integer.min

class FeedNotificationReceiver : BroadcastReceiver() {
	companion object {
		const val CHANNEL_ID_FEED = "notifications.feed"
	}

	override fun onReceive(context: Context, intent: Intent) {
		Log.d("NotificationReceiver", "NotificationReceiver received")

		val preferenceManager = PreferenceManager(context)
		if (!PreferenceUtils.getPrefBool(
				preferenceManager,
				"preference_notifications_feed_enable"
			)
		) return

		if (intent.hasExtra(LessonEventSetup.EXTRA_LONG_PROFILE_ID)) {
			val link = LinkDatabase.createInstance(context).getLink(
				intent.getLongExtra(LessonEventSetup.EXTRA_LONG_PROFILE_ID, -1)
			)
			link?.let {
				loadMessages(context, it, preferenceManager)
			}
		}
	}

	private fun loadMessages(
		context: Context,
		link: LinkDatabase.Link,
		preferenceManager: PreferenceManager
	) = GlobalScope.launch(Dispatchers.Main) {
		createNotificationChannel(context)
		val pendingIntent = PendingIntent.getActivity(
			context,
			0,
			Intent(context, MainActivity::class.java).apply {
				putExtra("info", true)
			},
			PendingIntent.FLAG_UPDATE_CURRENT
		)

		val messages = InfoCenterFragment.loadMessages(context, link)
		val lastTitle = PreferenceUtils.getPrefString(
			preferenceManager, "preference_last_title", ""
		)
		val lastTitleNotification = PreferenceUtils.getPrefString(
			preferenceManager, "preference_last_title_notification", ""
		)
		val limit = PreferenceUtils.getPrefInt(
			preferenceManager, "preference_notifications_feed_limit"
		)
		if (messages != null) {
			for (i in 0 until min(limit, messages.size)) {
				if (messages[i].title == lastTitle || messages[i].title == lastTitleNotification) {
					preferenceManager.defaultPrefs.edit().putString(
						"preference_last_title_notification", messages[0].title
					).apply()
					break
				}
				val message = HtmlCompat.fromHtml(
					messages[i].description ?: "", HtmlCompat.FROM_HTML_MODE_COMPACT
				)
				val builder = NotificationCompat.Builder(context, CHANNEL_ID_FEED)
					.setContentTitle(messages[i].title)
					.setContentText(message.toString().replace('\n', ' '))
					.setSmallIcon(R.drawable.all_infocenter)
					.setContentIntent(pendingIntent)
					.setStyle(NotificationCompat.BigTextStyle().bigText(message))

				with(NotificationManagerCompat.from(context)) {
					notify(messages[i].title.hashCode(), builder.build())
				}
			}
		}
	}

	private fun createNotificationChannel(context: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val name = context.getString(R.string.notifications_channel_feed)
			val descriptionText = context.getString(R.string.notifications_channel_feed_desc)
			val importance = NotificationManager.IMPORTANCE_DEFAULT
			val channel = NotificationChannel(CHANNEL_ID_FEED, name, importance).apply {
				description = descriptionText
			}
			val notificationManager: NotificationManager =
				context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}
}
