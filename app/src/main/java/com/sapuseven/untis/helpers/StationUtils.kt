package com.sapuseven.untis.helpers

import com.sapuseven.untis.helpers.config.PreferenceManager

object StationUtils {

	fun getFavorites(preferences: PreferenceManager): MutableSet<String?> {
		val immutableSet = preferences.defaultPrefs.getStringSet(
			"preference_stations",
			setOf("7007003", "7000037", "7001004")
		) ?: setOf()
		return mutableSetOf<String?>().apply {
			addAll(immutableSet)
		}
	}

	fun addFavorite(preferences: PreferenceManager, string: String?) {
		val set = getFavorites(preferences)
		set.add(string)
		setFavorites(preferences, set)
	}

	fun removeFavorite(preferences: PreferenceManager, string: String?) {
		val set = getFavorites(preferences)
		set.remove(string)
		setFavorites(preferences, set)
	}

	private fun setFavorites(preferences: PreferenceManager, set: Set<String?>) {
		preferences.defaultPrefs.edit().putStringSet("preference_stations", set).apply()
	}
}
