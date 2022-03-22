package com.sapuseven.untis.data.databases

import android.provider.BaseColumns

object LinkDatabaseContract {
	object Links : BaseColumns {
		const val TABLE_NAME = "links"
		const val COLUMN_NAME_RSSURL = "rssUrl"
		const val COLUMN_NAME_ICALURL = "iCalUrl"
		const val COLUMN_NAME_CREATED = "time_created"

		const val SQL_CREATE_ENTRIES_V1 =
			"CREATE TABLE $TABLE_NAME (" +
					"${BaseColumns._ID} INTEGER PRIMARY KEY," +
					"$COLUMN_NAME_RSSURL VARCHAR(128)," +
					"$COLUMN_NAME_ICALURL VARCHAR(128)," +
					"$COLUMN_NAME_CREATED DATETIME DEFAULT CURRENT_TIMESTAMP)"

		const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
	}
}
