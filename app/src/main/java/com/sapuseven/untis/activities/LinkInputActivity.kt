package com.sapuseven.untis.activities

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sapuseven.untis.R
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.dialogs.ProfileUpdateDialog
import com.sapuseven.untis.helpers.config.PreferenceManager
import kotlinx.android.synthetic.main.activity_link_input.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LinkInputActivity : BaseActivity() {

	companion object {
		private const val BACKUP_PREF_NAME = "linkDataInputBackup"

		private const val FRAGMENT_TAG_PROFILE_UPDATE = "profileUpdate"

		const val EXTRA_LONG_PROFILE_ID = "com.sapuseven.untis.activities.profileId"
		const val EXTRA_BOOLEAN_PROFILE_UPDATE = "com.sapuseven.untis.activities.profileupdate"
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


		existingLink?.let { link ->
			button_link_input_delete?.visibility = View.VISIBLE
			button_link_input_delete?.setOnClickListener {
				deleteProfile(link)
			}
		}

		focusFirstFreeField()

		setElementsEnabled(true)

		if (intent.getBooleanExtra(EXTRA_BOOLEAN_PROFILE_UPDATE, false)) {
			supportFragmentManager
				.beginTransaction()
				.replace(
					android.R.id.content, ProfileUpdateDialog(),
					FRAGMENT_TAG_PROFILE_UPDATE
				)
				.commit()

			loadData()
		}
	}

	private fun validate(): EditText? {
		if (edittext_link_input_rss?.text?.isEmpty() == true) {
			edittext_link_input_rss.error =
				getString(R.string.logindatainput_error_field_empty)
			return edittext_link_input_rss
		}
		if (edittext_link_input_ical?.text?.isEmpty() == true) {
			edittext_link_input_ical.error =
				getString(R.string.logindatainput_error_field_empty)
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

	public override fun onPause() {
		backupInput(this.getSharedPreferences(BACKUP_PREF_NAME, Context.MODE_PRIVATE))
		super.onPause()
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
		imageview_link_input_loadingstatusfailed?.visibility = View.GONE
		imageview_link_input_loadingstatussuccess?.visibility = View.GONE
		progressbar_link_input_loadingstatus?.visibility = View.VISIBLE
		textview_link_input_loadingstatus?.visibility = View.VISIBLE

		setElementsEnabled(false)
		GlobalScope.launch(Dispatchers.Main) {
			sendRequest()
		}
	}

	private suspend fun sendRequest() {
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
			progressbar_link_input_loadingstatus?.visibility = View.GONE
			imageview_link_input_loadingstatussuccess?.visibility = View.VISIBLE
			textview_link_input_loadingstatus?.text =
				getString(R.string.logindatainput_data_loaded)

			preferences.saveProfileId(linkId.toLong())

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
				getString(
					R.string.main_dialog_delete_profile_message,
					"HKA",
					"HKA"
				)
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

	private fun updateLoadingStatus(msg: String) {
		textview_link_input_loadingstatus?.text = msg
	}

	private fun stopLoadingAndShowError(msg: String) {
		updateLoadingStatus(msg)
		progressbar_link_input_loadingstatus?.visibility = View.GONE
		imageview_link_input_loadingstatusfailed?.visibility = View.VISIBLE
		setElementsEnabled(true)

		supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_PROFILE_UPDATE)
			?.let {
				supportFragmentManager
					.beginTransaction()
					.remove(it)
					.commit()
			}
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
