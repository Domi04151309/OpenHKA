package com.sapuseven.untis.activities

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import androidx.preference.*
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResult
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sapuseven.untis.BuildConfig
import com.sapuseven.untis.R
import com.sapuseven.untis.dialogs.AlertPreferenceDialog
import com.sapuseven.untis.dialogs.WeekRangePickerPreferenceDialog
import com.sapuseven.untis.helpers.AuthenticationHelper
import com.sapuseven.untis.helpers.SerializationUtils.getJSON
import com.sapuseven.untis.helpers.strings.StringLoaderSyncAuth
import com.sapuseven.untis.models.github.GithubUser
import com.sapuseven.untis.preferences.AlertPreference
import com.sapuseven.untis.preferences.WeekRangePickerPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.math.min

class SettingsActivity : BaseActivity(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
	private var profileId: Long? = null

	companion object {
		const val EXTRA_LONG_PROFILE_ID = "com.sapuseven.untis.activities.profileId"
		private const val REPOSITORY_URL_GITHUB = "https://github.com/Domi04151309/OpenHKA"
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		profileId = intent.extras?.getLong(EXTRA_LONG_PROFILE_ID)

		setupActionBar()
		setContentView(R.layout.activity_settings)

		if (savedInstanceState == null) {
			// Create the fragment only when the activity is created for the first time.
			// ie. not after orientation changes
			val fragment =
				supportFragmentManager.findFragmentByTag(PreferencesFragment.FRAGMENT_TAG)
					?: PreferencesFragment()
			val args = Bundle()
			profileId?.let { args.putLong(EXTRA_LONG_PROFILE_ID, it) }
			fragment.arguments = args

			supportFragmentManager
				.beginTransaction()
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
				.replace(
					R.id.framelayout_settings_content,
					fragment,
					PreferencesFragment.FRAGMENT_TAG
				)
				.commit()
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home)
			onBackPressed()
		return true
	}

	private fun setupActionBar() {
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
	}

	override fun onPreferenceStartScreen(
		preferenceFragmentCompat: PreferenceFragmentCompat,
		preferenceScreen: PreferenceScreen
	): Boolean {
		val fragment = PreferencesFragment()
		val args = Bundle()
		args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.key)
		profileId?.let { args.putLong(EXTRA_LONG_PROFILE_ID, it) }
		fragment.arguments = args

		supportFragmentManager
			.beginTransaction()
			.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
			.replace(R.id.framelayout_settings_content, fragment, preferenceScreen.key)
			.addToBackStack(preferenceScreen.title.toString())
			.commit()

		supportActionBar?.title = preferenceScreen.title
		return true
	}

	override fun onBackPressed() {
		super.onBackPressed()

		supportFragmentManager.run {
			supportActionBar?.title =
				if (backStackEntryCount > 0) getBackStackEntryAt(backStackEntryCount - 1).name else getString(
					R.string.activity_title_settings
				)
		}
	}

	class PreferencesFragment : PreferenceFragmentCompat() {

		companion object {
			const val FRAGMENT_TAG = "preference_fragment"
			const val DIALOG_FRAGMENT_TAG = "preference_dialog_fragment"
		}

		private var profileId: Long = 0

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			profileId = arguments?.getLong(EXTRA_LONG_PROFILE_ID) ?: 0
			if (profileId == 0L) {
				MaterialAlertDialogBuilder(requireContext())
					.setMessage("Invalid profile ID")
					.setPositiveButton("Exit") { _, _ ->
						activity?.finish()
					}
					.show()
			} else {
				preferenceManager.sharedPreferencesName = "preferences_$profileId"

				setPreferencesFromResource(R.xml.preferences, rootKey)

				when (rootKey) {
					null -> {
						findPreference<Preference>("preference_wear_os_support")?.setOnPreferenceClickListener {
							startActivity(Intent(context, WearOSActivity::class.java))
							true
						}
					}
					"preferences_general" -> {
						findPreference<SeekBarPreference>("preference_week_custom_display_length")?.apply {
							max =
								findPreference<WeekRangePickerPreference>("preference_week_custom_range")?.getPersistedStringSet(
									emptySet()
								)?.size?.zeroToNull
									?: this.max
						}

						findPreference<SwitchPreference>("preference_automute_enable")?.setOnPreferenceChangeListener { _, newValue ->
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && newValue == true) {
								(activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
									if (!isNotificationPolicyAccessGranted) {
										startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
										return@setOnPreferenceChangeListener false
									}
								}
							}
							true
						}

						findPreference<Preference>("preference_errors")?.setOnPreferenceClickListener {
							startActivity(Intent(context, ErrorsActivity::class.java))
							true
						}
					}
					"preferences_styling" -> {
						listOf(
							"preference_theme",
							"preference_dark_theme",
							"preference_dark_theme_oled"
						).forEach { key ->
							findPreference<Preference>(key)?.setOnPreferenceChangeListener { _, _ ->
								activity?.recreate()
								true
							}
						}

						findPreference<Preference>("preference_timetable_colors_reset")?.setOnPreferenceClickListener {
							MaterialAlertDialogBuilder(requireContext())
								.setTitle(R.string.preference_dialog_colors_reset_title)
								.setMessage(R.string.preference_dialog_colors_reset_text)
								.setPositiveButton(R.string.preference_timetable_colors_reset_button_positive) { _, _ ->
									(preferenceManager.sharedPreferences
										?: throw IllegalStateException()).edit().apply {
										listOf(
											"preference_background_regular",
											"preference_background_regular_past",
											"preference_background_exam",
											"preference_background_exam_past",
											"preference_background_irregular",
											"preference_background_irregular_past",
											"preference_background_cancelled",
											"preference_background_cancelled_past"
										).forEach {
											remove(it)
										}
										apply()
									}
									activity?.recreate()
								}
								.setNegativeButton(R.string.all_cancel) { _, _ -> }
								.show()
							true
						}
					}
					"preferences_notifications" -> {
						findPreference<Preference>("preference_notifications_enable")?.setOnPreferenceChangeListener { _, newValue ->
							if (newValue == false) clearNotifications()
							true
						}
						findPreference<Preference>("preference_notifications_clear")?.setOnPreferenceClickListener {
							clearNotifications()
							true
						}
					}
					"preferences_authentication" -> {
						val authInfo = findPreference<Preference>("preference_authentication_info")
							?: throw IllegalStateException()
						findPreference<Preference>("preference_authentication")?.apply {
							val auth =
								AuthenticationHelper((requireActivity() as BaseActivity).preferences)
							updateAuthPreference(auth, authInfo, this)
							setSummary(R.string.preference_authentication_desc)
							setOnPreferenceClickListener {
								if (auth.isLoggedIn()) {
									MaterialAlertDialogBuilder(context)
										.setTitle(R.string.preference_authentication_logout)
										.setMessage(R.string.preference_authentication_logout_question)
										.setPositiveButton(R.string.preference_authentication_logout) { _, _ ->
											auth.logout()
											updateAuthPreference(auth, authInfo, it)
										}
										.setNegativeButton(R.string.all_cancel) { _, _ -> }
										.show()
								} else auth.loginDialog {
									updateAuthPreference(auth, authInfo, it)
								}
								true
							}
						}
					}
					"preferences_info" -> {
						findPreference<Preference>("preference_info_app_version")?.apply {
							summary = requireContext().getString(
								R.string.preference_info_app_version_desc,
								BuildConfig.VERSION_NAME,
								BuildConfig.VERSION_CODE
							)
							setOnPreferenceClickListener {
								startActivity(
									Intent(
										Intent.ACTION_VIEW,
										Uri.parse("$REPOSITORY_URL_GITHUB/releases")
									)
								)
								true
							}
						}
						findPreference<Preference>("preference_info_github")?.apply {
							summary = REPOSITORY_URL_GITHUB
							setOnPreferenceClickListener {
								startActivity(
									Intent(
										Intent.ACTION_VIEW,
										Uri.parse(REPOSITORY_URL_GITHUB)
									)
								)
								true
							}
						}
						findPreference<Preference>("preference_info_license")?.setOnPreferenceClickListener {
							startActivity(
								Intent(
									Intent.ACTION_VIEW,
									Uri.parse("$REPOSITORY_URL_GITHUB/blob/main/LICENSE")
								)
							)
							true
						}
					}
					"preferences_contributors" -> {
						MaterialAlertDialogBuilder(requireContext())
							.setTitle(R.string.preference_info_privacy)
							.setMessage(R.string.preference_info_privacy_desc)
							.setPositiveButton(android.R.string.ok) { _, _ ->
								GlobalScope.launch(Dispatchers.Main) {
									"https://api.github.com/repos/sapuseven/betteruntis/contributors"
										.httpGet()
										.awaitStringResult()
										.fold({ original ->
											"https://api.github.com/repos/domi04151309/openhka/contributors"
												.httpGet()
												.awaitStringResult()
												.fold({ data ->
													showContributorList(true, original, data)
												}, {
													showContributorList(false)
												})
										}, {
											showContributorList(false)
										})
								}
							}
							.setNegativeButton(android.R.string.cancel) { _, _ ->
								parentFragmentManager.popBackStackImmediate()
							}
							.setNeutralButton(R.string.preference_info_privacy_policy) { _, _ ->
								parentFragmentManager.popBackStackImmediate()
								startActivity(
									Intent(
										Intent.ACTION_VIEW, Uri.parse(
											"https://docs.github.com/en/github/site-policy/github-privacy-statement"
										)
									)
								)
							}
							.show()
					}
				}
			}
		}

		private suspend fun showContributorList(
			success: Boolean,
			original: String = "",
			data: String = ""
		) {
			val preferenceScreen = this.preferenceScreen
			val indicator = findPreference<Preference>("preferences_contributors_indicator")
			if (success) {
				val contributors = (getJSON().decodeFromString<List<GithubUser>>(original) +
						getJSON().decodeFromString<List<GithubUser>>(data)).groupBy { it.login }
					.map { entry ->
						val user = entry.value[0]
						user.contributions = entry.value.sumOf { item -> item.contributions }
						user
					}

				preferenceScreen.removePreference(indicator ?: throw IllegalStateException())

				contributors.forEach { user ->
					preferenceScreen.addPreference(Preference(requireContext()).apply {
						GlobalScope.launch(Dispatchers.Main) {
							icon = loadProfileImage(user.avatar_url, resources)
						}
						title = user.login
						summary = resources.getQuantityString(
							R.plurals.preferences_contributors_contributions,
							user.contributions,
							user.contributions
						)
						setOnPreferenceClickListener {
							startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(user.html_url)))
							true
						}
					})
				}
			} else {
				indicator?.title = resources.getString(R.string.loading_failed)
			}
		}

		private suspend fun loadProfileImage(avatarUrl: String, resources: Resources): Drawable? {
			return avatarUrl
				.httpGet()
				.awaitByteArrayResult()
				.fold({
					BitmapDrawable(resources, BitmapFactory.decodeByteArray(it, 0, it.size))
				}, { null })
		}

		private fun clearNotifications() =
			(context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()

		private fun updateAuthPreference(
			auth: AuthenticationHelper,
			infoPreference: Preference,
			loginPreference: Preference
		) = GlobalScope.launch(Dispatchers.Main) {
			if (auth.isLoggedIn()) {
				val accountInfo = JSONObject(
					StringLoaderSyncAuth(
						WeakReference(context),
						"$API_URL/credential/v2/info",
						auth.get() ?: throw IllegalStateException()
					).load() ?: "{}"
				)

				infoPreference.title =
					accountInfo.optString("firstName") + ' ' + accountInfo.optString("lastName")
				infoPreference.summary =
					(accountInfo.optJSONObject("student")
						?: JSONObject()).optString("courseOfStudiesName")
				loginPreference.setTitle(R.string.preference_authentication_logout)
			} else {
				infoPreference.setTitle(R.string.unknown)
				infoPreference.setSummary(R.string.unknown)
				loginPreference.setTitle(R.string.preference_authentication_login)
			}
		}

		override fun onDisplayPreferenceDialog(preference: Preference) {
			parentFragmentManager.let { manager ->
				if (manager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) return

				when (preference) {
					is AlertPreference -> {
						val f: DialogFragment = AlertPreferenceDialog.newInstance(preference.key)
						f.setTargetFragment(this, 0)
						f.show(manager, DIALOG_FRAGMENT_TAG)
					}
					is WeekRangePickerPreference -> {
						val f: DialogFragment =
							WeekRangePickerPreferenceDialog.newInstance(preference.key) { positiveResult, selectedDays ->
								val visibleDaysPreference =
									findPreference<SeekBarPreference>("preference_week_custom_display_length")
								if (positiveResult) {
									visibleDaysPreference?.max = selectedDays.zeroToNull ?: 7
									visibleDaysPreference?.value = min(
										visibleDaysPreference?.value
											?: 0, selectedDays.zeroToNull ?: 7
									)
								}
							}
						f.setTargetFragment(this, 0)
						f.show(manager, DIALOG_FRAGMENT_TAG)
					}
					else -> super.onDisplayPreferenceDialog(preference)
				}
			}
		}
	}
}

private val Int.zeroToNull: Int?
	get() = if (this != 0) this else null
