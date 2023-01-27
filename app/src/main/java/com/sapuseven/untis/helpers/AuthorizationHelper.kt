package com.sapuseven.untis.helpers

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sapuseven.untis.helpers.config.PreferenceManager

class AuthorizationHelper(preferenceManager: PreferenceManager) {

	companion object {
		private const val KEY_USERNAME = "username"
		private const val KEY_PASSWORD = "password"
	}

	private val masterKeyAlias = MasterKey.Builder(
		preferenceManager.context,
		MasterKey.DEFAULT_MASTER_KEY_ALIAS
	).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

	private val _prefs: SharedPreferences = EncryptedSharedPreferences.create(
		preferenceManager.context,
		"preferences_${preferenceManager.currentProfileId()}",
		masterKeyAlias,
		EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
		EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
	)

	fun isLoggedIn(): Boolean = get() != null

	fun get(): Pair<String, String>? {
		return Pair(
			_prefs.getString(KEY_USERNAME, null) ?: return null,
			_prefs.getString(KEY_PASSWORD, null) ?: return null
		)
	}

	fun login(username: String, password: String) {
		_prefs.edit().putString(KEY_USERNAME, username).putString(KEY_PASSWORD, password).apply()
	}

	fun logout() {
		_prefs.edit().remove(KEY_USERNAME).remove(KEY_PASSWORD).apply()
	}
}
