package com.sapuseven.untis.helpers.strings

import android.content.Context
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import com.sapuseven.untis.interfaces.StringDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joda.time.Instant
import java.lang.ref.WeakReference


class StringLoader(
	private val context: WeakReference<Context>,
	private val stringDisplay: StringDisplay,
	private val link: String
) {
	companion object {
		const val FLAG_LOAD_CACHE: Int = 0b00000001
		const val FLAG_LOAD_SERVER: Int = 0b00000010

		const val CODE_CACHE_MISSING: Int = 1
		const val CODE_REQUEST_FAILED: Int = 2
	}

	private val cacheName = link.filter { it.isLetterOrDigit() }

	fun load(flags: Int = 0) =
		GlobalScope.launch(Dispatchers.Main) {

			if (flags and FLAG_LOAD_CACHE > 0)
				loadFromCache()
			if (flags and FLAG_LOAD_SERVER > 0)
				loadFromServer()
		}

	private fun loadFromCache() {
		val cache = StringCache(context, cacheName)

		if (cache.exists()) {
			cache.load()?.let { cacheObject ->
				stringDisplay.onStringLoaded(cacheObject.data)
			} ?: run {
				cache.delete()
				stringDisplay.onStringLoadingError(
					CODE_CACHE_MISSING
				)
			}
		} else {
			stringDisplay.onStringLoadingError(
				CODE_CACHE_MISSING
			)
		}
	}

	private suspend fun loadFromServer() {
		val cache = StringCache(context, cacheName)

		link.httpGet()
			.awaitStringResult()
			.fold({ data ->
				val timestamp = Instant.now().millis
				stringDisplay.onStringLoaded(data)
				cache.save(StringCache.CacheObject(timestamp, data))
			}, {
				stringDisplay.onStringLoadingError(
					CODE_REQUEST_FAILED
				)
			})
	}

	fun repeat(flags: Int = 0) {
		load(flags)
	}
}
