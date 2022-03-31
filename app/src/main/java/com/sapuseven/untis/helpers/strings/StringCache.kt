package com.sapuseven.untis.helpers.strings

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File
import java.lang.ref.WeakReference


class StringCache(private val context: WeakReference<Context>, private val name: String) {

	fun exists(): Boolean {
		return targetCacheFile().exists()
	}

	fun load(): CacheObject? {
		return try {
			Cbor.decodeFromByteArray<CacheObject>(
				targetCacheFile().readBytes()
			)
		} catch (e: Exception) {
			null
		}
	}

	fun save(items: CacheObject) {
		targetCacheFile().writeBytes(Cbor.encodeToByteArray(items))
	}

	private fun targetCacheFile(): File {
		return File(context.get()?.cacheDir, name)
	}

	override fun toString(): String {
		return name
	}

	fun delete() {
		targetCacheFile().delete()
	}

	@Serializable
	data class CacheObject(
		val timestamp: Long,
		val data: String
	)
}
