package com.sapuseven.untis.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
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
		private const val REQUEST_CODE_FRESHMAN = 6
	}

	private val linkDatabase = LinkDatabase.createInstance(this)
	private var lastBackPress: Long = 0
	private var profileId: Long = -1
	internal lateinit var profileLink: LinkDatabase.Link
	private lateinit var profileListAdapter: ProfileListAdapter

	private lateinit var drawerLayout: DrawerLayout
	private lateinit var navigationViewMain: NavigationView
	private lateinit var contentMain: View

	override fun onCreate(savedInstanceState: Bundle?) {
		hasOwnToolbar = true

		super.onCreate(savedInstanceState)

		if (!loadProfile())
			return

		setupNotifications()

		setContentView(R.layout.activity_main)

		drawerLayout = findViewById(R.id.drawer_layout)
		navigationViewMain = findViewById(R.id.navigationview_main)
		contentMain = findViewById(R.id.content_main)

		if (checkForCrashes()) {
			startActivityForResult(Intent(this, ErrorsActivity::class.java).apply {
				putExtra(ErrorsActivity.EXTRA_BOOLEAN_SHOW_CRASH_MESSAGE, true)
			}, REQUEST_CODE_ERRORS)
		} else {
			setupActionBar()
			setupNavDrawer()

			if (intent.hasExtra("info")) openInfoCenter()
			else if (intent.hasExtra("grades")) openGrades()
			else when (PreferenceUtils.getPrefString(preferences, "preference_launch_screen")) {
				"timetable" -> openPersonalTimetable()
				"info_center" -> openInfoCenter()
				"events" -> openEvents()
				"people" -> openPeople()
				"locations" -> openLocations()
				"study_places" -> openStudyPlaces()
				"mensa" -> openMensa()
				"stations" -> openStations()
				"grades" -> openGrades()
				"jobs" -> openJobs()
				"links" -> openLinks()
			}
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		if (intent.hasExtra("info")) openInfoCenter()
		else if (intent.hasExtra("grades")) openGrades()
	}

	override fun onResume() {
		super.onResume()
		refreshMessages(profileLink)
	}

	override fun onErrorLogFound() {
		// TODO: Extract string resources
		if (PreferenceUtils.getPrefBool(preferences, "preference_additional_error_messages"))
			Snackbar.make(contentMain, "Some errors have been found.", Snackbar.LENGTH_INDEFINITE)
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
		navigationViewMain.setNavigationItemSelectedListener(this)
		navigationViewMain.setCheckedItem(R.id.nav_show_personal)

		val header = navigationViewMain.getHeaderView(0)
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

		navigationViewMain.menu.findItem(R.id.nav_freshman_help).isVisible =
			PreferenceUtils.getPrefBool(
				preferences,
				"preference_freshman_show"
			)
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

	private fun refreshMessages(link: LinkDatabase.Link) =
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
		navigationViewMain.menu.findItem(R.id.nav_infocenter).setIcon(
			if (hasDot) R.drawable.ic_book_dot else R.drawable.ic_book
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
		val toolbarMain = findViewById<Toolbar>(R.id.toolbar_main)
		setSupportActionBar(toolbarMain)
		val toggle = ActionBarDrawerToggle(
			this,
			drawerLayout,
			toolbarMain,
			R.string.main_drawer_open,
			R.string.main_drawer_close
		)
		drawerLayout.addDrawerListener(toggle)
		toggle.syncState()
		toolbarMain.setNavigationIcon(R.drawable.ic_menu)
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
		window.statusBarColor = Color.TRANSPARENT

		supportFragmentManager.addOnBackStackChangedListener {
			if (supportFragmentManager.backStackEntryCount > 0) {
				toggle.isDrawerIndicatorEnabled = false
				supportActionBar?.setDisplayHomeAsUpEnabled(true)
				drawerLayout.setDrawerLockMode(
					DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
					GravityCompat.START
				)
				toolbarMain.setNavigationOnClickListener { onBackPressed() }
			} else {
				supportActionBar?.setDisplayHomeAsUpEnabled(false)
				toggle.isDrawerIndicatorEnabled = true
				toggle.syncState()
				drawerLayout.setDrawerLockMode(
					DrawerLayout.LOCK_MODE_UNLOCKED,
					GravityCompat.START
				)
				toolbarMain.setNavigationOnClickListener { openDrawer() }
			}
		}
	}

	private fun openMenuItem(itemId: Int, stringId: Int, fragment: Fragment) {
		navigationViewMain.setCheckedItem(itemId)
		supportActionBar?.setTitle(stringId)
		setFragment(fragment)
	}

	private fun openPersonalTimetable() = openMenuItem(
		R.id.nav_show_personal,
		R.string.app_name,
		TimetableFragment()
	)

	private fun openInfoCenter() = openMenuItem(
		R.id.nav_infocenter,
		R.string.activity_title_info_center,
		InfoCenterFragment()
	)

	private fun openEvents() = openMenuItem(
		R.id.nav_events,
		R.string.activity_title_events,
		EventFragment()
	)

	private fun openPeople() = openMenuItem(
		R.id.nav_people,
		R.string.activity_title_people,
		PeopleFragment()
	)

	private fun openLocations() = openMenuItem(
		R.id.nav_locations,
		R.string.activity_title_locations,
		LocationFragment()
	)

	private fun openStudyPlaces() = openMenuItem(
		R.id.nav_study_places,
		R.string.activity_title_study_places,
		StudyPlaceFragment()
	)

	private fun openMensa() = openMenuItem(
		R.id.nav_mensa,
		R.string.activity_title_mensa,
		MensaFragment()
	)

	private fun openStations() = openMenuItem(
		R.id.nav_stations,
		R.string.activity_title_stations,
		StationsFragment()
	)

	private fun openGrades() = openMenuItem(
		R.id.nav_grades,
		R.string.activity_title_grades,
		GradesFragment()
	)

	private fun openJobs() = openMenuItem(
		R.id.nav_jobs,
		R.string.activity_title_jobs,
		JobsFragment()
	)

	private fun openLinks() = openMenuItem(
		R.id.nav_links,
		R.string.activity_title_links,
		LinksFragment()
	)

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.nav_show_personal -> openPersonalTimetable()
			R.id.nav_freshman_help -> {
				startActivityForResult(Intent(Intent.ACTION_VIEW).apply {
					data = Uri.parse("https://ersti.hskampus.de/")
				}, REQUEST_CODE_FRESHMAN)
			}
			R.id.nav_infocenter -> openInfoCenter()
			R.id.nav_events -> openEvents()
			R.id.nav_people -> openPeople()
			R.id.nav_locations -> openLocations()
			R.id.nav_study_places -> openStudyPlaces()
			R.id.nav_mensa -> openMensa()
			R.id.nav_stations -> openStations()
			R.id.nav_grades -> openGrades()
			R.id.nav_jobs -> openJobs()
			R.id.nav_links -> openLinks()
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
		navigationViewMain.setCheckedItem(R.id.nav_show_personal)
		super.recreate()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, intent)

		when (requestCode) {
			REQUEST_CODE_SETTINGS, REQUEST_CODE_FRESHMAN -> recreate()
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
		if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
			closeDrawer()
		} else if (supportFragmentManager.backStackEntryCount > 0) {
			super.onBackPressed()
		} else if (System.currentTimeMillis() - 2000 > lastBackPress && PreferenceUtils.getPrefBool(
				preferences,
				"preference_double_tap_to_exit"
			)
		) {
			Snackbar.make(
				contentMain,
				R.string.main_press_back_double, 2000
			).show()
			lastBackPress = System.currentTimeMillis()
		} else {
			super.onBackPressed()
		}
	}

	internal fun setFullscreenDialogActionBar(res: Int) {
		supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
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

	private fun openDrawer(drawer: DrawerLayout = drawerLayout) =
		drawer.openDrawer(GravityCompat.START)

	private fun closeDrawer(drawer: DrawerLayout = drawerLayout) =
		drawer.closeDrawer(GravityCompat.START)
}
