package com.sapuseven.untis.helpers

import android.content.SharedPreferences
import android.view.LayoutInflater
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.helpers.config.PreferenceManager

class AuthenticationHelper(preferenceManager: PreferenceManager) {

	companion object {
		private const val KEY_USERNAME = "username"
		private const val KEY_PASSWORD = "password"
	}

	private val context = preferenceManager.context

	private val masterKeyAlias = MasterKey.Builder(
		context,
		MasterKey.DEFAULT_MASTER_KEY_ALIAS
	).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

	private val _prefs: SharedPreferences = EncryptedSharedPreferences.create(
		context,
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

	fun loginDialog(callback: () -> Unit) {
		val dialogView = LayoutInflater.from(context).inflate(
			R.layout.dialog_authentication,
			null,
			false
		)
		MaterialAlertDialogBuilder(context)
			.setView(dialogView)
			.setPositiveButton(R.string.all_ok) { _, _ ->
				login(
					dialogView.findViewById<TextInputLayout>(R.id.username).editText?.text?.toString()
						?: throw IllegalStateException(),
					dialogView.findViewById<TextInputLayout>(R.id.password).editText?.text?.toString()
						?: throw IllegalStateException()
				)
				callback()
			}
			.setNegativeButton(R.string.all_cancel) { _, _ -> }
			.show()
	}

	private fun login(username: String, password: String) {
		_prefs.edit().putString(KEY_USERNAME, username).putString(KEY_PASSWORD, password).apply()
	}

	fun logout() {
		_prefs.edit().remove(KEY_USERNAME).remove(KEY_PASSWORD).apply()
	}
}
