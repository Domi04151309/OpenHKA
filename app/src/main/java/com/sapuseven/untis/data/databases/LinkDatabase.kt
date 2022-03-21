package com.sapuseven.untis.data.databases

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.Cursor.FIELD_TYPE_INTEGER
import android.database.Cursor.FIELD_TYPE_STRING
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.sapuseven.untis.R
import com.sapuseven.untis.helpers.SerializationUtils.getJSON
import com.sapuseven.untis.helpers.LinkDatabaseQueryHelper.generateCreateTable
import com.sapuseven.untis.helpers.LinkDatabaseQueryHelper.generateDropTable
import com.sapuseven.untis.helpers.LinkDatabaseQueryHelper.generateValues
import com.sapuseven.untis.interfaces.TableModel
import com.sapuseven.untis.models.untis.UntisMasterData
import com.sapuseven.untis.models.untis.UntisSettings
import com.sapuseven.untis.models.untis.UntisUserData
import com.sapuseven.untis.models.untis.masterdata.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private const val DATABASE_VERSION = 1
private const val DATABASE_NAME = "linkdata.db"

class LinkDatabase private constructor(context: Context) :
	SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
	companion object {
		const val COLUMN_NAME_USER_ID = "_user_id"

		private var instance: LinkDatabase? = null

		fun createInstance(context: Context): LinkDatabase {
			return instance ?: LinkDatabase(context)
		}
	}

	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL(LinkDatabaseContract.Links.SQL_CREATE_ENTRIES_V5)
		db.execSQL(generateCreateTable<AbsenceReason>())
		db.execSQL(generateCreateTable<Department>())
		db.execSQL(generateCreateTable<Duty>())
		db.execSQL(generateCreateTable<EventReason>())
		db.execSQL(generateCreateTable<EventReasonGroup>())
		db.execSQL(generateCreateTable<ExcuseStatus>())
		db.execSQL(generateCreateTable<Holiday>())
		db.execSQL(generateCreateTable<Klasse>())
		db.execSQL(generateCreateTable<Room>())
		db.execSQL(generateCreateTable<Subject>())
		db.execSQL(generateCreateTable<Teacher>())
		db.execSQL(generateCreateTable<TeachingMethod>())
		db.execSQL(generateCreateTable<SchoolYear>())
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		var currentVersion = oldVersion

		while (currentVersion < newVersion) {
			when (currentVersion) {
			}

			currentVersion++
		}
	}

	fun resetDatabase(db: SQLiteDatabase) {
		db.execSQL(LinkDatabaseContract.Links.SQL_DELETE_ENTRIES)

		db.execSQL(generateDropTable<AbsenceReason>())
		db.execSQL(generateDropTable<Department>())
		db.execSQL(generateDropTable<Duty>())
		db.execSQL(generateDropTable<EventReason>())
		db.execSQL(generateDropTable<EventReasonGroup>())
		db.execSQL(generateDropTable<ExcuseStatus>())
		db.execSQL(generateDropTable<Holiday>())
		db.execSQL(generateDropTable<Klasse>())
		db.execSQL(generateDropTable<Room>())
		db.execSQL(generateDropTable<Subject>())
		db.execSQL(generateDropTable<Teacher>())
		db.execSQL(generateDropTable<TeachingMethod>())
		db.execSQL(generateDropTable<SchoolYear>())
	}

	fun addLink(link: Link): Long? {
		val db = writableDatabase

		val values = ContentValues()
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_PROFILENAME, link.profileName)
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_RSSURL, link.rssUrl)
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_ICALURL, link.iCalUrl)
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_USER, link.user)
		values.put(
			LinkDatabaseContract.Links.COLUMN_NAME_TIMEGRID,
			getJSON().encodeToString<TimeGrid>(link.timeGrid)
		)
		values.put(
			LinkDatabaseContract.Links.COLUMN_NAME_MASTERDATATIMESTAMP,
			link.masterDataTimestamp
		)

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
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_PROFILENAME, link.profileName)
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_RSSURL, link.rssUrl)
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_ICALURL, link.iCalUrl)
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_USER, link.user)
		values.put(
			LinkDatabaseContract.Links.COLUMN_NAME_TIMEGRID,
			getJSON().encodeToString<TimeGrid>(link.timeGrid)
		)
		values.put(
			LinkDatabaseContract.Links.COLUMN_NAME_MASTERDATATIMESTAMP,
			link.masterDataTimestamp
		)

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
				LinkDatabaseContract.Links.COLUMN_NAME_PROFILENAME,
				LinkDatabaseContract.Links.COLUMN_NAME_RSSURL,
				LinkDatabaseContract.Links.COLUMN_NAME_ICALURL,
				LinkDatabaseContract.Links.COLUMN_NAME_USER,
				LinkDatabaseContract.Links.COLUMN_NAME_TIMEGRID,
				LinkDatabaseContract.Links.COLUMN_NAME_MASTERDATATIMESTAMP,
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
			cursor.getString(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_PROFILENAME)),
			cursor.getString(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_RSSURL)),
			cursor.getString(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_ICALURL)),
			cursor.getStringOrNull(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_USER)),
			getJSON().decodeFromString(
				cursor.getString(
					cursor.getColumnIndexOrThrow(
						LinkDatabaseContract.Links.COLUMN_NAME_TIMEGRID
					)
				)
			),
			cursor.getLong(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_MASTERDATATIMESTAMP)),
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
				LinkDatabaseContract.Links.COLUMN_NAME_PROFILENAME,
				LinkDatabaseContract.Links.COLUMN_NAME_RSSURL,
				LinkDatabaseContract.Links.COLUMN_NAME_ICALURL,
				LinkDatabaseContract.Links.COLUMN_NAME_USER,
				LinkDatabaseContract.Links.COLUMN_NAME_TIMEGRID,
				LinkDatabaseContract.Links.COLUMN_NAME_MASTERDATATIMESTAMP,
				LinkDatabaseContract.Links.COLUMN_NAME_CREATED
			), null, null, null, null, LinkDatabaseContract.Links.COLUMN_NAME_CREATED + " DESC"
		)

		if (cursor.moveToFirst()) {
			do {
				links.add(
					Link(
						cursor.getLongOrNull(cursor.getColumnIndexOrThrow(BaseColumns._ID)),
						cursor.getString(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_PROFILENAME)),
						cursor.getString(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_RSSURL)),
						cursor.getString(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_ICALURL)),
						cursor.getStringOrNull(cursor.getColumnIndexOrThrow(LinkDatabaseContract.Links.COLUMN_NAME_USER)),
						getJSON().decodeFromString<TimeGrid>(
							cursor.getString(
								cursor.getColumnIndexOrThrow(
									UserDatabaseContract.Users.COLUMN_NAME_TIMEGRID
								)
							)
						),
						cursor.getLong(cursor.getColumnIndexOrThrow(UserDatabaseContract.Users.COLUMN_NAME_MASTERDATATIMESTAMP)),
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

	fun setAdditionalLinkData(
		userId: Long,
		masterData: UntisMasterData
	) {
		val db = writableDatabase
		db.beginTransaction()

		listOf(
			AbsenceReason.TABLE_NAME to masterData.absenceReasons,
			Department.TABLE_NAME to masterData.departments,
			Duty.TABLE_NAME to masterData.duties,
			EventReason.TABLE_NAME to masterData.eventReasons,
			EventReasonGroup.TABLE_NAME to masterData.eventReasonGroups,
			ExcuseStatus.TABLE_NAME to masterData.excuseStatuses,
			Holiday.TABLE_NAME to masterData.holidays,
			Klasse.TABLE_NAME to masterData.klassen,
			Room.TABLE_NAME to masterData.rooms,
			Subject.TABLE_NAME to masterData.subjects,
			Teacher.TABLE_NAME to masterData.teachers,
			TeachingMethod.TABLE_NAME to masterData.teachingMethods,
			SchoolYear.TABLE_NAME to masterData.schoolyears
		).forEach { refreshAdditionalLinkData(db, userId, it.first, it.second) }

		val values = ContentValues()
		values.put(LinkDatabaseContract.Links.COLUMN_NAME_MASTERDATATIMESTAMP, masterData.timeStamp)
		db.update(
			LinkDatabaseContract.Links.TABLE_NAME,
			values,
			BaseColumns._ID + "=?",
			arrayOf(userId.toString())
		)

		db.setTransactionSuccessful()
		db.endTransaction()
		db.close()
	}

	private fun refreshAdditionalLinkData(
		db: SQLiteDatabase,
		userId: Long,
		tableName: String,
		items: List<TableModel>
	) {
		db.delete(tableName, "$COLUMN_NAME_USER_ID=?", arrayOf(userId.toString()))
		items.forEach { data -> db.insert(tableName, null, generateValues(userId, data)) }
	}


	inline fun <reified T : TableModel> getAdditionalLinkData(
		userId: Long,
		table: TableModel
	): Map<Int, T>? {
		val db = readableDatabase

		val cursor = db.query(
			table.tableName,
			table.generateValues().keySet().toTypedArray(), "$COLUMN_NAME_USER_ID=?",
			arrayOf(userId.toString()), null, null, "id DESC"
		)

		if (!cursor.moveToFirst())
			return null

		val result = mutableMapOf<Int, T>()

		if (cursor.moveToFirst()) {
			do {
				val data = table.parseCursor(cursor) as T
				result[(data as TableModel).elementId] = data
			} while (cursor.moveToNext())
		}

		cursor.close()
		db.close()

		return result.toMap()
	}

	class Link(
		val id: Long? = null,
		val profileName: String = "",
		val rssUrl: String,
		val iCalUrl: String,
		val user: String? = null,
		val timeGrid: TimeGrid,
		val masterDataTimestamp: Long,
		val created: Long? = null
	) {
		fun getDisplayedName(context: Context): String = profileName
	}
}

private fun Cursor.getIntOrNull(columnIndex: Int): Int? {
	return if (getType(columnIndex) == FIELD_TYPE_INTEGER)
		getInt(columnIndex)
	else null
}

private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
	return if (getType(columnIndex) == FIELD_TYPE_INTEGER)
		getLong(columnIndex)
	else null
}

private fun Cursor.getStringOrNull(columnIndex: Int): String? {
	return if (getType(columnIndex) == FIELD_TYPE_STRING)
		getString(columnIndex)
	else null
}
