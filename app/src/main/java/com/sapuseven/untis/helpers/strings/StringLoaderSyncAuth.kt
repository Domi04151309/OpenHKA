package com.sapuseven.untis.helpers.strings

import android.content.Context
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import org.joda.time.Instant
import java.lang.ref.WeakReference


class StringLoaderSyncAuth(
	context: WeakReference<Context>,
	link: String,
	private val authentication: Pair<String, String>
): StringLoaderSync(context, link) {

	override suspend fun loadFromServer(): String? {
		val cache = StringCache(context, cacheName)
		return link.httpGet()
			.authentication().basic(authentication.first , authentication.second)
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
