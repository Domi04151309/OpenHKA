package com.sapuseven.untis.helpers.timetable

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File
import java.lang.ref.WeakReference


class TimetableCache(val context: WeakReference<Context>) {

	fun exists(): Boolean {
		return targetCacheFile()?.exists() ?: false
	}

	fun load(): CacheObject? {
		return try {
			Cbor.decodeFromByteArray<CacheObject>(
				targetCacheFile()?.readBytes() ?: ByteArray(
					0
				)
			)
		} catch (e: Exception) {
			null
		}
	}

	fun save(items: CacheObject) {
		targetCacheFile()?.writeBytes(Cbor.encodeToByteArray(items))
	}

	private fun targetCacheFile(): File? {
		return File(context.get()?.cacheDir, "default")
	}

	override fun toString(): String {
		return "default"
	}

	fun delete() {
		targetCacheFile()?.delete()
	}

	@Serializable
	data class CacheObject(
		val timestamp: Long,
		val data: String
	)
}
