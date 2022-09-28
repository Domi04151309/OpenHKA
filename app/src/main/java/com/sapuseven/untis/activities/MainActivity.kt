package com.sapuseven.untis.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.ProfileListAdapter
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.fragments.*
import com.sapuseven.untis.helpers.config.PreferenceUtils
import com.sapuseven.untis.receivers.NotificationSetup.Companion.EXTRA_BOOLEAN_MANUAL
import com.sapuseven.untis.receivers.StartupReceiver
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MainActivity :
	BaseActivity(),
	NavigationView.OnNavigationItemSelectedListener {

	companion object {
		private const val REQUEST_CODE_SETTINGS = 2
		private const val REQUEST_CODE_LOGINDATAINPUT_ADD = 3
		private const val REQUEST_CODE_LOGINDATAINPUT_EDIT = 4
		private const val REQUEST_CODE_ERRORS = 5
	}

	private val linkDatabase = LinkDatabase.createInstance(this)
	private var lastBackPress: Long = 0
	private var profileId: Long = -1
	internal lateinit var profileLink: LinkDatabase.Link
	private lateinit var profileListAdapter: ProfileListAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		hasOwnToolbar = true

		super.onCreate(savedInstanceState)

		if (!loadProfile())
			return

		setupNotifications()

		setContentView(R.layout.activity_main)

		if (checkForCrashes()) {
			startActivityForResult(Intent(this, ErrorsActivity::class.java).apply {
				putExtra(ErrorsActivity.EXTRA_BOOLEAN_SHOW_CRASH_MESSAGE, true)
			}, REQUEST_CODE_ERRORS)
		} else {
			setupActionBar()
			setupNavDrawer()

			setFragment(TimetableFragment())
		}
	}

	override fun onResume() {
		super.onResume()
		refreshMessages(profileLink, navigationview_main)
	}

	override fun onErrorLogFound() {
		// TODO: Extract string resources
		if (PreferenceUtils.getPrefBool(preferences, "preference_additional_error_messages"))
			Snackbar.make(content_main, "Some errors have been found.", Snackbar.LENGTH_INDEFINITE)
				.setAction("Show") {
					startActivity(Intent(this, ErrorsActivity::class.java))
				}
				.show()
	}

	private fun login() {
		val loginIntent = Intent(this, LinkInputActivity::class.java)
		startActivityForResult(loginIntent, REQUEST_CODE_LOGINDATAINPUT_ADD)
		finish()
	}

	private fun setupNotifications() {
		val intent = Intent(this, StartupReceiver::class.java)
		intent.putExtra(EXTRA_BOOLEAN_MANUAL, true)
		sendBroadcast(intent)
	}

	private fun setupNavDrawer() {
		navigationview_main.setNavigationItemSelectedListener(this)
		navigationview_main.setCheckedItem(R.id.nav_show_personal)

		val header = navigationview_main.getHeaderView(0)
		val dropdown =
			header.findViewById<ConstraintLayout>(R.id.constraintlayout_mainactivitydrawer_dropdown)
		val dropdownView =
			header.findViewById<LinearLayout>(R.id.linearlayout_mainactivitydrawer_dropdown_view)
		val dropdownImage =
			header.findViewById<ImageView>(R.id.imageview_mainactivitydrawer_dropdown_arrow)
		val dropdownList =
			header.findViewById<RecyclerView>(R.id.recyclerview_mainactivitydrawer_profile_list)

		profileListAdapter = ProfileListAdapter(
			linkDatabase.getAllLinks().toMutableList(),
			{ view ->
				toggleProfileDropdown(dropdownView, dropdownImage, dropdownList)
				switchToProfile(profileListAdapter.itemAt(dropdownList.getChildLayoutPosition(view)))
			}
		) { view ->
			closeDrawer()
			editProfile(profileListAdapter.itemAt(dropdownList.getChildLayoutPosition(view)))
			true
		}
		dropdownList.adapter = profileListAdapter
		dropdown.setOnClickListener {
			toggleProfileDropdown(dropdownView, dropdownImage, dropdownList)
		}

		val profileListAdd =
			header.findViewById<ConstraintLayout>(R.id.constraintlayout_mainactivitydrawer_add)
		profileListAdd.setOnClickListener {
			closeDrawer()
			addProfile()
		}
	}

	private fun toggleProfileDropdown(
		dropdownView: ViewGroup,
		dropdownImage: ImageView,
		dropdownList: RecyclerView
	) {
		if (dropdownImage.scaleY < 0) {
			dropdownImage.scaleY = 1F
			dropdownView.visibility = View.GONE
		} else {
			dropdownImage.scaleY = -1F

			dropdownList.setHasFixedSize(true)
			dropdownList.layoutManager = LinearLayoutManager(this)

			dropdownView.visibility = View.VISIBLE
		}
	}

	private fun addProfile() {
		val loginIntent = Intent(this, LinkInputActivity::class.java)
		startActivityForResult(loginIntent, REQUEST_CODE_LOGINDATAINPUT_ADD)
	}

	private fun editProfile(link: LinkDatabase.Link) {
		val loginIntent = Intent(this, LinkInputActivity::class.java)
			.putExtra(LinkInputActivity.EXTRA_LONG_PROFILE_ID, link.id)
		startActivityForResult(loginIntent, REQUEST_CODE_LOGINDATAINPUT_EDIT)
	}

	private fun switchToProfile(link: LinkDatabase.Link) {
		profileId = link.id!!
		preferences.saveProfileId(profileId)
		preferences.reload(profileId)
		if (loadProfile()) {
			closeDrawer()
			recreate()
		}
	}

	private fun refreshMessages(link: LinkDatabase.Link, navigationView: NavigationView) =
		GlobalScope.launch(Dispatchers.Main) {
			InfoCenterFragment.loadMessages(this@MainActivity, link)?.let {
				setInfoCenterDot(
					it.isNotEmpty()
							&& it[0].title != preferences.defaultPrefs.getString(
						"preference_last_title",
						""
					)
				)
			}
		}

	internal fun setInfoCenterDot(hasDot: Boolean) {
		navigationview_main.menu.findItem(R.id.nav_infocenter).setIcon(
			if (hasDot) R.drawable.all_infocenter_dot else R.drawable.all_infocenter
		)
	}

	private fun loadProfile(): Boolean {
		if (linkDatabase.getLinkCount() < 1) {
			login()
			return false
		}

		profileId = preferences.currentProfileId()
		if (profileId == 0L || linkDatabase.getLink(profileId) == null)
			profileId = linkDatabase.getAllLinks()[0].id
				?: 0 // Fall back to the first user if an invalid user id is saved
		profileLink = linkDatabase.getLink(profileId) ?: return false

		preferences.saveProfileId(profileId)
		preferences.reload(profileId)

		return true
	}

	private fun setupActionBar() {
		setSupportActionBar(toolbar_main)
		val toggle = ActionBarDrawerToggle(
			this,
			drawer_layout,
			toolbar_main,
			R.string.main_drawer_open,
			R.string.main_drawer_close
		)
		drawer_layout.addDrawerListener(toggle)
		toggle.syncState()
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
		window.statusBarColor = Color.TRANSPARENT

		supportFragmentManager.addOnBackStackChangedListener {
			if (supportFragmentManager.backStackEntryCount > 0) {
				toggle.isDrawerIndicatorEnabled = false
				supportActionBar?.setDisplayHomeAsUpEnabled(true)
				drawer_layout.setDrawerLockMode(
					DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
					GravityCompat.START
				)
				toolbar_main.setNavigationOnClickListener { onBackPressed() }
				// TODO: Set actionBar title to match fragment
			} else {
				supportActionBar?.setDisplayHomeAsUpEnabled(false)
				toggle.isDrawerIndicatorEnabled = true
				toggle.syncState()
				drawer_layout.setDrawerLockMode(
					DrawerLayout.LOCK_MODE_UNLOCKED,
					GravityCompat.START
				)
				toolbar_main.setNavigationOnClickListener { openDrawer() }
				// TODO: Set actionBar title to default
			}
		}
	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.nav_show_personal -> {
				setDefaultActionBar()
				setFragment(TimetableFragment())
			}
			R.id.nav_infocenter -> {
				supportActionBar?.setTitle(R.string.activity_title_info_center)
				setFragment(InfoCenterFragment())
			}
			R.id.nav_mail -> {
				supportActionBar?.setTitle(R.string.activity_title_mail)
				setFragment(MailFragment())
			}
			R.id.nav_events -> {
				supportActionBar?.setTitle(R.string.activity_title_events)
				setFragment(EventFragment())
			}
			R.id.nav_people -> {
				supportActionBar?.setTitle(R.string.activity_title_people)
				setFragment(PeopleFragment())
			}
			R.id.nav_locations -> {
				supportActionBar?.setTitle(R.string.activity_title_locations)
				setFragment(LocationFragment())
			}
			R.id.nav_study_places -> {
				supportActionBar?.setTitle(R.string.activity_title_study_places)
				setFragment(StudyPlaceFragment())
			}
			R.id.nav_mensa -> {
				supportActionBar?.setTitle(R.string.activity_title_mensa)
				setFragment(MensaFragment())
			}
			R.id.nav_stations -> {
				supportActionBar?.setTitle(R.string.activity_title_stations)
				setFragment(StationsFragment())
			}
			R.id.nav_links -> {
				supportActionBar?.setTitle(R.string.activity_title_links)
				setFragment(LinksFragment())
			}
			R.id.nav_settings -> {
				val i = Intent(this, SettingsActivity::class.java)
				i.putExtra(SettingsActivity.EXTRA_LONG_PROFILE_ID, profileId)
				startActivityForResult(i, REQUEST_CODE_SETTINGS)
			}
		}

		closeDrawer()
		return true
	}

	override fun recreate() {
		for (fragment in supportFragmentManager.fragments) {
			supportFragmentManager.beginTransaction().remove(fragment!!).commit()
		}
		navigationview_main?.setCheckedItem(R.id.nav_show_personal)
		super.recreate()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, intent)

		when (requestCode) {
			REQUEST_CODE_SETTINGS -> recreate()
			REQUEST_CODE_LOGINDATAINPUT_ADD ->
				if (resultCode == Activity.RESULT_OK)
					recreate()
			REQUEST_CODE_LOGINDATAINPUT_EDIT ->
				if (resultCode == Activity.RESULT_OK)
					recreate()
			REQUEST_CODE_ERRORS -> recreate()
		}
	}

	override fun onBackPressed() {
		if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
			closeDrawer(drawer_layout)
		} else if (supportFragmentManager.backStackEntryCount > 0) {
			super.onBackPressed()
		} else if (System.currentTimeMillis() - 2000 > lastBackPress && PreferenceUtils.getPrefBool(
				preferences,
				"preference_double_tap_to_exit"
			)
		) {
			Snackbar.make(
				content_main,
				R.string.main_press_back_double, 2000
			).show()
			lastBackPress = System.currentTimeMillis()
		} else {
			super.onBackPressed()
		}
	}

	internal fun setFullscreenDialogActionBar(res: Int) {
		supportActionBar?.setHomeAsUpIndicator(R.drawable.all_close)
		supportActionBar?.setTitle(res)
	}

	internal fun setDefaultActionBar(res: Int = R.string.app_name) {
		supportActionBar?.setTitle(res)
	}

	private fun setFragment(fragment: Fragment) {
		supportFragmentManager.beginTransaction().run {
			setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
			replace(R.id.content_main, fragment, fragment::class.simpleName)
			commit()
		}
	}

	private fun openDrawer(drawer: DrawerLayout = drawer_layout) =
		drawer.openDrawer(GravityCompat.START)

	private fun closeDrawer(drawer: DrawerLayout = drawer_layout) =
		drawer.closeDrawer(GravityCompat.START)
}
