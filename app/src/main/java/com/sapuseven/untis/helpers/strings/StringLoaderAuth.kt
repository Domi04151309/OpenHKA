package com.sapuseven.untis.helpers.strings

import android.content.Context
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import com.sapuseven.untis.interfaces.StringDisplay
import org.joda.time.Instant
import java.lang.ref.WeakReference


class StringLoaderAuth(
	context: WeakReference<Context>,
	stringDisplay: StringDisplay,
	link: String,
	private val authentication: Pair<String, String>
): StringLoader(context, stringDisplay, link) {

	override suspend fun loadFromServer() {
		val cache = StringCache(context, cacheName)

		link.httpGet()
			.authentication().basic(authentication.first , authentication.second)
			.awaitStringResult()
			.fold({ data ->
				val timestamp = Instant.now().millis
				stringDisplay.onStringLoaded(data)
				cache.save(StringCache.CacheObject(timestamp, data))
			}, {
				stringDisplay.onStringLoadingError(
					CODE_REQUEST_FAILED,
					this
				)
			})
	}
}
