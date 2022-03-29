package com.sapuseven.untis.wear.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.common.data.FreezableUtils
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.sapuseven.untis.R
import com.sapuseven.untis.data.databases.LinkDatabase

class ConnectActivity : WearableActivity(), DataClient.OnDataChangedListener {

	companion object {
		private const val UNTIS_LOGIN = "/untis_login"
		private const val UNTIS_SUCCESS = "/untis_success"
	}

	private var status: Byte = 0x01
	private var existingLinkId: Long? = null

	private lateinit var linkDatabase: LinkDatabase

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_connect)

		linkDatabase = LinkDatabase.createInstance(this)

		Wearable.getDataClient(this).dataItems.addOnCompleteListener {
			if (it.isSuccessful) {
				it.result?.get(0)?.let { item ->
					if (item.uri.path == UNTIS_LOGIN) saveItems(DataMapItem.fromDataItem(item).dataMap)
				}
			}
			it.result?.release()
		}
	}

	override fun onStart() {
		super.onStart()
		Wearable.getDataClient(this).addListener(this)
	}

	override fun onStop() {
		super.onStop()
		Wearable.getDataClient(this).removeListener(this)
	}

	override fun onDataChanged(dataEvents: DataEventBuffer) {
		FreezableUtils.freezeIterable(dataEvents).forEach {
			val item = it.dataItem
			if (item.uri.path == UNTIS_LOGIN) saveItems(DataMapItem.fromDataItem(item).dataMap)
		}
		dataEvents.release()
	}

	private fun saveItems(map: DataMap) {
		if (!map.containsKey("rss") || !map.containsKey("iCal")) return

		PreferenceManager.getDefaultSharedPreferences(this).edit()
			.putBoolean("signed_in", true).apply()

		linkDatabase.addLink(
			LinkDatabase.Link(
				existingLinkId,
				map.getString("rss") ?: "",
				map.getString("iCal") ?: ""
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
