package com.sapuseven.untis.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sapuseven.untis.R
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.helpers.config.PreferenceManager
import kotlinx.android.synthetic.main.activity_link_input.*

class LinkInputActivity : BaseActivity() {

	companion object {
		private const val BACKUP_PREF_NAME = "linkInputBackup"
		private const val HELP_URL = "https://github.com/Domi04151309/SimpleHKA/wiki/Help"
		private const val PRIVACY_POLICY_URL = "https://github.com/Domi04151309/SimpleHKA/wiki/Privacy-Policy"

		const val EXTRA_LONG_PROFILE_ID = "com.sapuseven.untis.activities.profileId"
	}

	private var existingLink: LinkDatabase.Link? = null
	private var existingLinkId: Long? = null

	private lateinit var linkDatabase: LinkDatabase

	override fun onCreate(savedInstanceState: Bundle?) {
		if (intent.hasExtra(EXTRA_LONG_PROFILE_ID)) {
			existingLinkId = intent.getLongExtra(EXTRA_LONG_PROFILE_ID, 0)
			preferences = PreferenceManager(this, existingLinkId!!)
		}

		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_link_input)

		linkDatabase = LinkDatabase.createInstance(this)
		existingLinkId?.let { id ->
			existingLink = linkDatabase.getLink(id)
			existingLink?.let { link ->
				restoreInput(link)
			}
		} ?: run {
			this.getSharedPreferences(BACKUP_PREF_NAME, Context.MODE_PRIVATE)?.let {
				restoreInput(it)
			}
		}

		title =
			getString(if (existingLinkId == null) R.string.logindatainput_title_add else R.string.logindatainput_title_edit)

		button_link_input_done?.setOnClickListener {
			validate()?.requestFocus() ?: run { loadData() }
		}

		button_link_input_privacy_policy?.setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(PRIVACY_POLICY_URL)))
		}

		button_link_input_help?.setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(HELP_URL)))
		}

		existingLink?.let { link ->
			button_link_input_delete?.visibility = View.VISIBLE
			button_link_input_delete?.setOnClickListener {
				deleteProfile(link)
			}
		}

		focusFirstFreeField()

		setElementsEnabled(true)
	}

	private fun validate(): EditText? {
		if (edittext_link_input_rss?.text?.isEmpty() == true) {
			edittext_link_input_rss.error =
				getString(R.string.link_input_error_field_empty)
			return edittext_link_input_rss
		}
		if (!URLUtil.isValidUrl(edittext_link_input_rss.text.toString())) {
			edittext_link_input_rss.error =
				getString(R.string.link_input_error_invalid_url)
			return edittext_link_input_rss
		}
		if (edittext_link_input_ical?.text?.isEmpty() == true) {
			edittext_link_input_ical.error =
				getString(R.string.link_input_error_field_empty)
			return edittext_link_input_ical
		}
		if (!URLUtil.isValidUrl(edittext_link_input_ical.text.toString())) {
			edittext_link_input_ical.error =
				getString(R.string.link_input_error_invalid_url)
			return edittext_link_input_ical
		}
		return null
	}

	private fun focusFirstFreeField() {
		when {
			edittext_link_input_rss?.text?.isEmpty() == true -> edittext_link_input_rss as EditText
			edittext_link_input_ical?.text?.isEmpty() == true -> edittext_link_input_ical as EditText
			else -> edittext_link_input_rss as EditText
		}.requestFocus()
	}

	public override fun onStop() {
		backupInput(this.getSharedPreferences(BACKUP_PREF_NAME, Context.MODE_PRIVATE))
		super.onStop()
	}

	private fun clearInput(prefs: SharedPreferences) {
		val editor = prefs.edit()
		editor.remove("edittext_link_input_rss")
		editor.remove("edittext_link_input_ical")
		editor.apply()
	}

	private fun backupInput(prefs: SharedPreferences) {
		val editor = prefs.edit()
		editor.putString(
			"edittext_link_input_rss",
			edittext_link_input_rss?.text.toString()
		)
		editor.putString(
			"edittext_link_input_ical",
			edittext_link_input_ical?.text.toString()
		)
		editor.apply()
	}

	private fun restoreInput(prefs: SharedPreferences) {
		edittext_link_input_rss?.setText(
			prefs.getString(
				"edittext_link_input_rss",
				""
			)
		)
		edittext_link_input_ical?.setText(
			prefs.getString(
				"edittext_link_input_ical",
				""
			)
		)
	}

	private fun restoreInput(link: LinkDatabase.Link) {
		edittext_link_input_rss?.setText(link.rssUrl)
		edittext_link_input_ical?.setText(link.iCalUrl)
	}

	private fun loadData() {
		setElementsEnabled(false)
		sendRequest()
	}

	private fun sendRequest() {
		val link = LinkDatabase.Link(
			existingLinkId,
			edittext_link_input_rss?.text.toString(),
			edittext_link_input_ical?.text.toString()
		)

		val linkId =
			if (existingLinkId == null) linkDatabase.addLink(link) else linkDatabase.editLink(
				link
			)

		linkId?.let {
			preferences.saveProfileId(linkId.toLong())
			clearInput(this.getSharedPreferences(BACKUP_PREF_NAME, Context.MODE_PRIVATE))

			setResult(Activity.RESULT_OK)
			finish()
		}

		setElementsEnabled(true)
	}

	//TODO: hardcoded string
	private fun deleteProfile(link: LinkDatabase.Link) {
		MaterialAlertDialogBuilder(this)
			.setTitle(getString(R.string.main_dialog_delete_profile_title))
			.setMessage(
				getString(R.string.main_dialog_delete_profile_message)
			)
			.setNegativeButton(getString(R.string.all_cancel), null)
			.setPositiveButton(getString(R.string.all_delete)) { _, _ ->
				linkDatabase.deleteLink(link.id!!)
				preferences.deleteProfile(link.id)
				setResult(RESULT_OK)
				finish()
			}
			.show()
	}

	override fun onBackPressed() {
		setElementsEnabled(false)
		super.onBackPressed()
	}

	private fun setElementsEnabled(enabled: Boolean) {
		edittext_link_input_rss?.isEnabled = enabled
		edittext_link_input_ical?.isEnabled = enabled
		button_link_input_done?.isEnabled = enabled
	}
}
