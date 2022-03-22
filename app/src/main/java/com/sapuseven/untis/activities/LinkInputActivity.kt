package com.sapuseven.untis.activities

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sapuseven.untis.R
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.data.databases.UserDatabase
import com.sapuseven.untis.dialogs.ProfileUpdateDialog
import com.sapuseven.untis.helpers.ErrorMessageDictionary
import com.sapuseven.untis.helpers.SerializationUtils
import com.sapuseven.untis.helpers.api.LoginDataInfo
import com.sapuseven.untis.helpers.api.LoginErrorInfo
import com.sapuseven.untis.helpers.api.LoginHelper
import com.sapuseven.untis.models.UntisSchoolInfo
import com.sapuseven.untis.models.untis.masterdata.TimeGrid
import kotlinx.android.synthetic.main.activity_link_input.*
import kotlinx.android.synthetic.main.activity_logindatainput.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString

class LinkInputActivity : BaseActivity() {

	companion object {
		private const val BACKUP_PREF_NAME = "linkDataInputBackup"

		private const val FRAGMENT_TAG_PROFILE_UPDATE = "profileUpdate"
	}

	private var existingLink: LinkDatabase.Link? = null
	private var existingLinkId: Long? = null

	private lateinit var linkDatabase: LinkDatabase

	override fun onCreate(savedInstanceState: Bundle?) {
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


		/*existingLink?.let { link ->
			button_logindatainput_delete?.visibility = View.VISIBLE
			button_logindatainput_delete?.setOnClickListener {
				deleteProfile(link)
			}
		}*/

		focusFirstFreeField()

		setElementsEnabled(true)

		if (intent.getBooleanExtra(LoginDataInputActivity.EXTRA_BOOLEAN_PROFILE_UPDATE, false)) {
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
		//TODO: validate
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
		imageview_logindatainput_loadingstatusfailed?.visibility = View.GONE
		imageview_logindatainput_loadingstatussuccess?.visibility = View.GONE
		progressbar_logindatainput_loadingstatus?.visibility = View.VISIBLE
		textview_logindatainput_loadingstatus?.visibility = View.VISIBLE

		setElementsEnabled(false)
		GlobalScope.launch(Dispatchers.Main) {
			sendRequest()
		}
	}

	private suspend fun sendRequest() {
		//TODO: do something here
		setResult(Activity.RESULT_OK)
		finish()
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
		textview_logindatainput_loadingstatus?.text = msg
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
