package com.sapuseven.untis.helpers.strings

import android.content.Context
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import org.joda.time.Instant
import java.lang.ref.WeakReference


class StringLoaderSync(
	private val context: WeakReference<Context>,
	private val link: String
) {

	private val cacheName = link.filter { it.isLetterOrDigit() }

	suspend fun load(): String? {
		var result = loadFromServer()
		if (result == null) result = loadFromCache()
		return result
	}

	private fun loadFromCache(): String? {
		val cache = StringCache(context, cacheName)
		if (cache.exists()) {
			cache.load()?.let { cacheObject ->
				return cacheObject.data
			} ?: run {
				cache.delete()
				return null
			}
		} else {
			return null
		}
	}

	private suspend fun loadFromServer(): String? {
		val cache = StringCache(context, cacheName)
		return link.httpGet()
			.awaitStringResult()
			.fold({ data ->
				val timestamp = Instant.now().millis
				cache.save(StringCache.CacheObject(timestamp, data))
				data
			}, {
				null
			})
	}
}
