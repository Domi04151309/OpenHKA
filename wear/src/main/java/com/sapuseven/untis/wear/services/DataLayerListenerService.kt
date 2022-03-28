package com.sapuseven.untis.wear.services

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.gms.common.data.FreezableUtils
import com.google.android.gms.wearable.*

class DataLayerListenerService : WearableListenerService() {

	companion object {
		private const val UNTIS_LOGIN = "/untis_login"
	}

	override fun onDataChanged(dataEvents: DataEventBuffer) {
		val events = FreezableUtils.freezeIterable(dataEvents)
		events.forEach {
			val item = it.dataItem
			if (item.uri.path == UNTIS_LOGIN) {
				val map = DataMapItem.fromDataItem(item).dataMap

				val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
				editor.putString(
					"link_input_rss",
					map.getString("link_input_rss")
				)
				editor.putString(
					"link_input_ical",
					map.getString("link_input_ical")
				)
				editor.apply()

				LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("LOGIN_SUCCESS"))
			}
		}
	}
}
