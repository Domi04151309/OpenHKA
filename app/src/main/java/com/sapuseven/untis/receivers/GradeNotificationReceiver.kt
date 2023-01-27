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
import com.sapuseven.untis.fragments.GradesFragment
import com.sapuseven.untis.helpers.AuthenticationHelper
import com.sapuseven.untis.helpers.config.PreferenceManager
import com.sapuseven.untis.helpers.config.PreferenceUtils
import com.sapuseven.untis.helpers.strings.StringLoaderSyncAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class GradeNotificationReceiver : BroadcastReceiver() {
	companion object {
		const val CHANNEL_ID_GRADES = "notifications.grades"
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
	}

	override fun onReceive(context: Context, intent: Intent) {
		Log.d("NotificationReceiver", "NotificationReceiver received")

		val preferenceManager = PreferenceManager(context)
		if (!PreferenceUtils.getPrefBool(
				preferenceManager,
				"preference_notifications_grades_enable"
			)
		) return

		loadGrades(context, preferenceManager)
	}

	private fun loadGrades(
		context: Context,
		preferenceManager: PreferenceManager
	) = GlobalScope.launch(Dispatchers.Main) {
		val auth = AuthenticationHelper(preferenceManager)
		if (!auth.isLoggedIn()) return@launch

		createNotificationChannel(context)
		val pendingIntent = PendingIntent.getActivity(
			context,
			0,
			Intent(context, MainActivity::class.java).apply {
				putExtra("grades", true)
			},
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		val grades = GradesFragment.parseGrades(StringLoaderSyncAuth(
			WeakReference(context),
			"${API_URL}/grades/all",
			auth.get() ?: throw IllegalStateException()
		).load() ?: return@launch)
		val lastGradeCount = PreferenceUtils.getPrefInt(
			preferenceManager, "preference_last_grade_count", 0
		)

		if (lastGradeCount < grades.list.size) {
			preferenceManager.defaultPrefs.edit().putInt(
				"preference_last_grade_count", grades.list.size
			).apply()
			val title = context.resources.getString(R.string.activity_title_grades)
			val builder = NotificationCompat.Builder(context, CHANNEL_ID_GRADES)
				.setContentTitle(title)
				.setContentText(context.resources.getString(R.string.notifications_text_grades))
				.setSmallIcon(R.drawable.ic_school)
				.setContentIntent(pendingIntent)

			with(NotificationManagerCompat.from(context)) {
				notify(title.hashCode(), builder.build())
			}
		}
	}

	private fun createNotificationChannel(context: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val name = context.getString(R.string.notifications_channel_feed)
			val descriptionText = context.getString(R.string.activity_title_grades)
			val importance = NotificationManager.IMPORTANCE_DEFAULT
			val channel = NotificationChannel(CHANNEL_ID_GRADES, name, importance).apply {
				description = descriptionText
			}
			val notificationManager: NotificationManager =
				context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}
}
