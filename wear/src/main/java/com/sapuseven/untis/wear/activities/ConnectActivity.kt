package com.sapuseven.untis.wear.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.sapuseven.untis.R
import com.sapuseven.untis.data.databases.LinkDatabase

class ConnectActivity : WearableActivity() {

	companion object {
		private const val UNTIS_SUCCESS = "/untis_success"
	}

	private val receiver = object : BroadcastReceiver() {
		override fun onReceive(c: Context, intent: Intent) {
			val prefs = PreferenceManager.getDefaultSharedPreferences(c)
			sendRequest(
				prefs.getString("edittext_link_input_rss", "") ?: "",
				prefs.getString("edittext_link_input_ical", "") ?: ""
			)
		}
	}

	private var status: Byte = 0x01
	private var existingLinkId: Long? = null

	private lateinit var linkDatabase: LinkDatabase

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_connect)

		linkDatabase = LinkDatabase.createInstance(this)
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(receiver, IntentFilter("LOGIN_SUCCESS"))
	}

	internal fun sendRequest(rss: String, iCal: String) {
		linkDatabase.addLink(
			LinkDatabase.Link(
				existingLinkId,
				rss,
				iCal
			)
		)

		SendMessage(this@ConnectActivity, status).start()
		startActivity(Intent(this@ConnectActivity, MainActivity::class.java))
		finish()
	}

	internal class SendMessage(private val c: Context, private val status: Byte) : Thread() {

		override fun run() {
			val nodeListTask: Task<List<Node>> =
				Wearable.getNodeClient(c.applicationContext).connectedNodes
			try {
				val nodes: List<Node> = Tasks.await(nodeListTask)
				nodes.forEach {
					val sendMessageTask: Task<Int> = Wearable.getMessageClient(c)
						.sendMessage(it.id, UNTIS_SUCCESS, byteArrayOf(status))
					try {
						Tasks.await(sendMessageTask)
					} catch (e: Exception) {
					}
				}
			} catch (e: Exception) {
			}
		}
	}
}
