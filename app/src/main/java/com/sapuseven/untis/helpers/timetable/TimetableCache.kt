package com.sapuseven.untis.helpers.timetable

import android.content.Context
import com.sapuseven.untis.models.untis.UntisDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File
import java.lang.ref.WeakReference


class TimetableCache(val context: WeakReference<Context>) {
	private var target: CacheTarget? = null

	fun setTarget(startDate: UntisDate, endDate: UntisDate) {
		target = CacheTarget(startDate, endDate)
	}

	fun exists(): Boolean {
		return targetCacheFile(target)?.exists() ?: false
	}

	fun load(): CacheObject? {
		return try {
			Cbor.decodeFromByteArray<CacheObject>(
				targetCacheFile(target)?.readBytes() ?: ByteArray(
					0
				)
			)
		} catch (e: Exception) {
			null
		}
	}

	fun save(items: CacheObject) {
		targetCacheFile(target)?.writeBytes(Cbor.encodeToByteArray(items))
	}

	private fun targetCacheFile(target: CacheTarget?): File? {
		return File(context.get()?.cacheDir, target?.getName() ?: "default")
	}

	override fun toString(): String {
		return target?.getName() ?: "null"
	}

	fun delete() {
		targetCacheFile(target)?.delete()
	}

	@Serializable
	data class CacheObject(
		val timestamp: Long,
		val data: String
	)

	private inner class CacheTarget(
		val startDate: UntisDate,
		val endDate: UntisDate
	) {
		fun getName(): String {
			return String.format("%s-%s", startDate, endDate)
		}
	}
}
