package com.sapuseven.untis.helpers.drawables

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResult
import com.github.kittinunf.fuel.httpGet
import org.joda.time.Instant
import java.lang.ref.WeakReference


object DrawableLoader {
	const val FLAG_LOAD_CACHE: Int = 0b00000001
	const val FLAG_LOAD_SERVER: Int = 0b00000010

	private fun cacheName(input: String): String = input.filter { it.isLetterOrDigit() }

	suspend fun load(context: WeakReference<Context>, url: String, flags: Int = 0): Drawable? {
		return if (flags and FLAG_LOAD_CACHE > 0)
			loadFromCache(context, url)
		else
			loadFromServer(context, url)
	}

	private suspend fun loadFromCache(
		context: WeakReference<Context>,
		url: String
	): Drawable? {
		val cache = DrawableCache(context, cacheName(url))
		if (cache.exists()) {
			cache.load()?.let { cacheObject ->
				return BitmapDrawable(
					context.get()?.resources,
					BitmapFactory.decodeByteArray(cacheObject.data, 0, cacheObject.data.size)
				)
			} ?: run {
				cache.delete()
				return loadFromServer(context, url)
			}
		} else {
			return loadFromServer(context, url)
		}
	}

	private suspend fun loadFromServer(
		context: WeakReference<Context>,
		url: String
	): Drawable? {
		val cache = DrawableCache(context, cacheName(url))
		return url.httpGet()
			.awaitByteArrayResult()
			.fold({
				cache.save(DrawableCache.CacheObject(Instant.now().millis, it))
				BitmapDrawable(
					context.get()?.resources,
					BitmapFactory.decodeByteArray(it, 0, it.size)
				)
			}, { null })
	}
}
