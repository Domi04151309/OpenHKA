package com.sapuseven.untis.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.*
import com.sapuseven.untis.R
import com.sapuseven.untis.data.databases.LinkDatabase

class WearOSActivity : BaseActivity() {

	companion object {
		private const val UNTIS_LOGIN = "/untis_login"
		private const val SUCCESS: Byte = 0x01
	}

	private val receiver = object : BroadcastReceiver() {
		override fun onReceive(c: Context, intent: Intent) {
			if (intent.getByteExtra("message", 0x00) == SUCCESS) {
				statusImg.setImageResource(R.drawable.ic_check)
				title.text = resources.getString(R.string.preference_wear_os_support_success)
				summary.text =
					resources.getString(R.string.preference_wear_os_support_success_desc)
			} else {
				statusImg.setImageResource(R.drawable.ic_close_red)
				title.text = resources.getString(R.string.preference_wear_os_support_error)
				summary.text =
					resources.getString(R.string.preference_wear_os_support_error_desc)
			}
		}
	}

	lateinit var statusImg: ImageView
	lateinit var title: TextView
	lateinit var summary: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_wear_os)
		supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
		statusImg = findViewById(R.id.status)
		title = findViewById(R.id.title)
		summary = findViewById(R.id.description)

		ContextCompat.registerReceiver(
			this,
			receiver,
			IntentFilter("LOGIN_SUCCESS"),
			ContextCompat.RECEIVER_NOT_EXPORTED
		)
	}

	override fun onResume() {
		super.onResume()

		val linkDatabase = LinkDatabase.createInstance(this)
		var profileId = preferences.currentProfileId()
		if (profileId == 0L || linkDatabase.getLink(profileId) == null) profileId =
			linkDatabase.getAllLinks()[0].id ?: 0
		val profileLink = linkDatabase.getLink(profileId)

		val dataClient: DataClient = Wearable.getDataClient(this)
		val putDataMapRequest = PutDataMapRequest.create(UNTIS_LOGIN)
		val map = putDataMapRequest.dataMap
		map.putString("rss", profileLink?.rssUrl ?: "")
		map.putString("iCal", profileLink?.iCalUrl ?: "")
		val request = putDataMapRequest.asPutDataRequest()
		request.setUrgent()
		dataClient.putDataItem(request)
	}
}
