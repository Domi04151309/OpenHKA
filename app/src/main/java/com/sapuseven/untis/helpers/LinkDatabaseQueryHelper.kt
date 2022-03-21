package com.sapuseven.untis.helpers

import android.content.ContentValues
import android.provider.BaseColumns
import com.sapuseven.untis.annotations.Table
import com.sapuseven.untis.annotations.TableColumn
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.interfaces.TableModel
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

object LinkDatabaseQueryHelper {
	inline fun <reified T : Any> generateCreateTable(): String? {
		val tableName = generateTableName<T>()
		tableName?.let { _ ->
			var query = "CREATE TABLE $tableName (${BaseColumns._ID} INTEGER PRIMARY KEY, ${LinkDatabase.COLUMN_NAME_USER_ID} INTEGER"
			T::class.declaredMemberProperties.forEach { field ->
				val column = field.javaField?.getAnnotation(TableColumn::class.java)
				column?.let { query += ", " + field.name + " " + column.type }
			}
			query += ")"

			return query
		}
		return null
	}

	inline fun <reified T : Any> generateDropTable(): String? {
		val tableName = generateTableName<T>()
		tableName?.let { return "DROP TABLE IF EXISTS $tableName" }
		return null
	}

	inline fun <reified T : Any> generateTableName(): String? {
		return (T::class.annotations.find { it is Table } as? Table)?.name
	}

	fun generateValues(userId: Long, data: TableModel): ContentValues {
		val values = data.generateValues()
		values.put(LinkDatabase.COLUMN_NAME_USER_ID, userId)
		return values
	}
}
