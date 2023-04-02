package com.sapuseven.untis.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.helpers.config.PreferenceManager


class LinkInputActivity : BaseActivity() {

	companion object {
		private const val REQUEST_CODE_FEED_LIST = 1
		private const val REQUEST_CODE_ICAL_LIST = 2
		private const val BACKUP_PREF_NAME = "linkInputBackup"
		private const val HELP_URL = "https://github.com/Domi04151309/OpenHKA/wiki/Help"
		private const val PRIVACY_POLICY_URL =
			"https://github.com/Domi04151309/OpenHKA/wiki/Privacy-Policy"
		private const val NO_LINK_FEED = "https://www.h-ka.de/feed.rss"
		private const val NO_LINK_ICAL =
			"https://www.iwi.hs-karlsruhe.de/hskampus-broker/api/calendar/schedule/current"

		const val EXTRA_LONG_PROFILE_ID = "com.sapuseven.untis.activities.profileId"
	}

	private var existingLink: LinkDatabase.Link? = null
	private var existingLinkId: Long? = null

	private lateinit var linkDatabase: LinkDatabase

	private lateinit var buttonLinkInputDone: FloatingActionButton
	private lateinit var editTextLinkInputRss: EditText
	private lateinit var editTextLinkInputICal: EditText

	override fun onCreate(savedInstanceState: Bundle?) {
		if (intent.hasExtra(EXTRA_LONG_PROFILE_ID)) {
			existingLinkId = intent.getLongExtra(EXTRA_LONG_PROFILE_ID, 0)
			preferences = PreferenceManager(this, existingLinkId!!)
		}

		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_link_input)

		buttonLinkInputDone = findViewById(R.id.button_link_input_done)
		editTextLinkInputRss = findViewById(R.id.edittext_link_input_rss)
		editTextLinkInputICal = findViewById(R.id.edittext_link_input_ical)

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

		buttonLinkInputDone.setOnClickListener {
			validate()?.requestFocus() ?: run { loadData() }
		}

		findViewById<Button>(R.id.button_link_input_privacy_policy).setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(PRIVACY_POLICY_URL)))
		}

		findViewById<Button>(R.id.button_link_input_help).setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(HELP_URL)))
		}

		findViewById<Button>(R.id.button_link_input_skip).setOnClickListener {
			editTextLinkInputRss.setText(NO_LINK_FEED)
			editTextLinkInputICal.setText(NO_LINK_ICAL)
			validate()?.requestFocus() ?: run { loadData() }
		}

		findViewById<TextInputLayout>(R.id.textinputlayout_link_input_rss).setEndIconOnClickListener {
			startActivityForResult(
				Intent(this, FeedLinkChooserActivity::class.java),
				REQUEST_CODE_FEED_LIST
			)
		}

		findViewById<TextInputLayout>(R.id.textinputlayout_link_input_ical).setEndIconOnClickListener {
			startActivityForResult(
				Intent(this, ICalLinkChooserActivity::class.java),
				REQUEST_CODE_ICAL_LIST
			)
		}

		existingLink?.let { link ->
			findViewById<FloatingActionButton>(R.id.button_link_input_delete).let { button ->
				button.visibility = View.VISIBLE
				button.setOnClickListener { deleteProfile(link) }
			}
		}

		focusFirstFreeField()

		setElementsEnabled(true)
	}

	private fun validate(): EditText? {
		if (editTextLinkInputRss.text?.isEmpty() == true) {
			editTextLinkInputRss.error =
				getString(R.string.link_input_error_field_empty)
			return editTextLinkInputRss
		}
		if (!URLUtil.isValidUrl(editTextLinkInputRss.text.toString())) {
			editTextLinkInputRss.error =
				getString(R.string.link_input_error_invalid_url)
			return editTextLinkInputRss
		}
		if (editTextLinkInputICal.text?.isEmpty() == true) {
			editTextLinkInputICal.error =
				getString(R.string.link_input_error_field_empty)
			return editTextLinkInputICal
		}
		if (!URLUtil.isValidUrl(editTextLinkInputICal.text.toString())) {
			editTextLinkInputICal.error =
				getString(R.string.link_input_error_invalid_url)
			return editTextLinkInputICal
		}
		return null
	}

	private fun focusFirstFreeField() {
		when {
			editTextLinkInputRss.text?.isEmpty() == true -> editTextLinkInputRss as EditText
			editTextLinkInputICal.text?.isEmpty() == true -> editTextLinkInputICal as EditText
			else -> editTextLinkInputRss as EditText
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
			editTextLinkInputRss.text.toString()
		)
		editor.putString(
			"edittext_link_input_ical",
			editTextLinkInputICal.text.toString()
		)
		editor.apply()
	}

	private fun restoreInput(prefs: SharedPreferences) {
		editTextLinkInputRss.setText(
			prefs.getString(
				"edittext_link_input_rss",
				""
			)
		)
		editTextLinkInputICal.setText(
			prefs.getString(
				"edittext_link_input_ical",
				""
			)
		)
	}

	private fun restoreInput(link: LinkDatabase.Link) {
		editTextLinkInputRss.setText(link.rssUrl)
		editTextLinkInputICal.setText(link.iCalUrl)
	}

	private fun loadData() {
		setElementsEnabled(false)
		sendRequest()
	}

	private fun sendRequest() {
		val link = LinkDatabase.Link(
			existingLinkId,
			editTextLinkInputRss.text.toString(),
			editTextLinkInputICal.text.toString()
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

	@Deprecated("")
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_CODE_FEED_LIST && resultCode == RESULT_OK) {
			editTextLinkInputRss.setText(data?.getStringExtra("link"))
		} else if (requestCode == REQUEST_CODE_ICAL_LIST && resultCode == RESULT_OK) {
			editTextLinkInputICal.setText(data?.getStringExtra("link"))
		}
	}

	override fun onBackPressed() {
		setElementsEnabled(false)
		super.onBackPressed()
	}

	private fun setElementsEnabled(enabled: Boolean) {
		editTextLinkInputRss.isEnabled = enabled
		editTextLinkInputICal.isEnabled = enabled
		buttonLinkInputDone.isEnabled = enabled
	}
}
