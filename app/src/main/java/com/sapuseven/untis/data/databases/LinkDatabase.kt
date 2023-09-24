package com.sapuseven.untis.data.databases

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.Cursor.FIELD_TYPE_INTEGER
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

private const val DATABASE_VERSION = 1
private const val DATABASE_NAME = "linkdata.db"

class LinkDatabase private constructor(context: Context) :
	SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
	companion object {
		private var instance: LinkDatabase? = null

		fun createInstance(context: Context): LinkDatabase {
			return instance ?: LinkDatabase(context)
		}
	}

	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL(LinkDatabaseContract.Links.SQL_CREATE_ENTRIES_V1)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		var currentVersion = oldVersion

		while (currentVersion < newVersion) {
			@Suppress("ControlFlowWithEmptyBody")
			when (currentVersion) {
			}

			currentVersion++
		}
	}

	fun resetDatabase(db: SQLiteDatabase) {
		db.execSQL(LinkDatabaseContract.Links.SQL_DELETE_ENTRIES)
	}

	fun addLink(link: Link): Long? {
		val db = writableDatabase

		val values = ContentValues()
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_RSSURL, link.rssUrl)
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_ICALURL, link.iCalUrl)

		val id = db.insert(LinkDatabaseContract.Links.TABLE_NAME, null, values)

		db.close()

		return if (id == -1L)
			null
		else
			id
	}

	fun editLink(link: Link): Long? {
		val db = writableDatabase

		val values = ContentValues()
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_RSSURL, link.rssUrl)
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_ICALURL, link.iCalUrl)

		db.update(
			LinkDatabaseContract.Links.TABLE_NAME,
			values,
			BaseColumns._ID + "=?",
			arrayOf(link.id.toString())
		)
		db.close()

		return link.id
	}

	fun deleteLink(linkId: Long) {
		val db = writableDatabase
		db.delete(
			LinkDatabaseContract.Links.TABLE_NAME,
			BaseColumns._ID + "=?",
			arrayOf(linkId.toString())
		)
		db.close()
	}

	fun getLink(id: Long): Link? {
		val db = this.readableDatabase

		val cursor = db.query(
			LinkDatabaseContract.Links.TABLE_NAME,
			arrayOf(
				BaseColumns._ID,
				LinkDatabaseContract.Links.COLUMN_NAME_RSSURL,
				LinkDatabaseContract.Links.COLUMN_NAME_ICALURL,
				LinkDatabaseContract.Links.COLUMN_NAME_CREATED
			),
			BaseColumns._ID + "=?",
			arrayOf(id.toString()),
			null,
			null,
			LinkDatabaseContract.Links.COLUMN_NAME_CREATED + " DESC"
		)

		if (!cursor.moveToFirst())
			return null

		val link = Link(
			id,
			cursor.getString(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_RSSURL)),
			cursor.getString(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_ICALURL)),
			cursor.getLongOrNull(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_CREATED))
		)
		cursor.close()
		db.close()

		return link
	}

	fun getAllLinks(): List<Link> {
		val links = ArrayList<Link>()
		val db = this.readableDatabase

		val cursor = db.query(
			LinkDatabaseContract.Links.TABLE_NAME,
			arrayOf(
				BaseColumns._ID,
				LinkDatabaseContract.Links.COLUMN_NAME_RSSURL,
				LinkDatabaseContract.Links.COLUMN_NAME_ICALURL,
				LinkDatabaseContract.Links.COLUMN_NAME_CREATED
			), null, null, null, null, LinkDatabaseContract.Links.COLUMN_NAME_CREATED + " DESC"
		)

		if (cursor.moveToFirst()) {
			do {
				links.add(
					Link(
						cursor.getLongOrNull(cursor.getColumnIndexOrThrow(BaseColumns._ID)),
						cursor.getString(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_RSSURL)),
						cursor.getString(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_ICALURL)),
						cursor.getLongOrNull(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_CREATED))
					)
				)
			} while (cursor.moveToNext())
		}

		cursor.close()
		db.close()

		return links
	}

	fun getLinkCount(): Int {
		val db = this.readableDatabase

		val cursor = db.query(
			LinkDatabaseContract.Links.TABLE_NAME,
			arrayOf(BaseColumns._ID), null, null, null, null, null
		)

		val count = cursor.count
		cursor.close()
		db.close()

		return count
	}

	class Link(
		val id: Long? = null,
		val rssUrl: String,
		val iCalUrl: String,
		val created: Long? = null
	)
}

internal fun Cursor.getLongOrNull(columnIndex: Int): Long? {
	return if (getType(columnIndex) == FIELD_TYPE_INTEGER)
		getLong(columnIndex)
	else null
}
