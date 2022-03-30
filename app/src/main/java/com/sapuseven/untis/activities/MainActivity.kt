package com.sapuseven.untis.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.antonious.materialdaypicker.MaterialDayPicker
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.ProfileListAdapter
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.dialogs.DatePickerDialog
import com.sapuseven.untis.dialogs.ErrorReportingDialog
import com.sapuseven.untis.fragments.TimetableItemDetailsFragment
import com.sapuseven.untis.helpers.ConversionUtils
import com.sapuseven.untis.helpers.ErrorMessageDictionary
import com.sapuseven.untis.helpers.config.PreferenceUtils
import com.sapuseven.untis.helpers.timetable.TimetableLoader
import com.sapuseven.untis.interfaces.TimetableDisplay
import com.sapuseven.untis.models.untis.timetable.Period
import com.sapuseven.untis.preferences.RangePreference
import com.sapuseven.untis.receivers.NotificationSetup.Companion.EXTRA_BOOLEAN_MANUAL
import com.sapuseven.untis.receivers.StartupReceiver
import com.sapuseven.untis.views.weekview.WeekView
import com.sapuseven.untis.views.weekview.WeekViewDisplayable
import com.sapuseven.untis.views.weekview.WeekViewEvent
import com.sapuseven.untis.views.weekview.listeners.EventClickListener
import com.sapuseven.untis.views.weekview.listeners.ScaleListener
import com.sapuseven.untis.views.weekview.listeners.ScrollListener
import com.sapuseven.untis.views.weekview.listeners.TopLeftCornerClickListener
import com.sapuseven.untis.views.weekview.loaders.WeekViewLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.Instant
import org.joda.time.LocalDate
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


class MainActivity :
	BaseActivity(),
	NavigationView.OnNavigationItemSelectedListener,
	WeekViewLoader.PeriodChangeListener<TimegridItem>,
	EventClickListener<TimegridItem>,
	TopLeftCornerClickListener,
	TimetableDisplay {

	companion object {
		private const val MINUTE_MILLIS: Int = 60 * 1000
		private const val HOUR_MILLIS: Int = 60 * MINUTE_MILLIS
		private const val DAY_MILLIS: Int = 24 * HOUR_MILLIS

		private const val REQUEST_CODE_ROOM_FINDER = 1
		private const val REQUEST_CODE_SETTINGS = 2
		private const val REQUEST_CODE_LOGINDATAINPUT_ADD = 3
		private const val REQUEST_CODE_LOGINDATAINPUT_EDIT = 4
		private const val REQUEST_CODE_ERRORS = 5

		private const val PERSISTENT_INT_ZOOM_LEVEL = "persistent_zoom_level"

		private const val FRAGMENT_TAG_LESSON_INFO = "com.sapuseven.untis.fragments.lessoninfo"
	}

	private val linkDatabase = LinkDatabase.createInstance(this)
	private var lastBackPress: Long = 0
	private var profileId: Long = -1
	private var weeklyTimetableItems: WeeklyTimetableItems? = null
	private var lastPickedDate: DateTime? = null
	private val weekViewRefreshHandler = Handler(Looper.getMainLooper())
	private var timetableLoader: TimetableLoader? = null
	private lateinit var profileLink: LinkDatabase.Link
	private lateinit var profileListAdapter: ProfileListAdapter
	private lateinit var weekView: WeekView<TimegridItem>

	private val weekViewUpdate = object : Runnable {
		override fun run() {
			weekView.invalidate()
			weekViewRefreshHandler.postDelayed(this, 60 * 1000)
		}
	}

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

			setupViews()
			setupHours()

			setupTimetableLoader()
			showPersonalTimetable()
		}
	}

	override fun onPause() {
		weekViewRefreshHandler.removeCallbacks(weekViewUpdate)
		super.onPause()
	}

	override fun onResume() {
		super.onResume()
		if (timetableLoader == null) return

		refreshMessages(profileLink, navigationview_main)

		if (::weekView.isInitialized) {
			setupWeekViewConfig()

			weekViewRefreshHandler.post(weekViewUpdate)
		}
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

	private fun showPersonalTimetable(): Boolean {
		return setTarget()
	}

	private fun setupNotifications() {
		val intent = Intent(this, StartupReceiver::class.java)
		intent.putExtra(EXTRA_BOOLEAN_MANUAL, true)
		sendBroadcast(intent)
	}

	private fun setupTimetableLoader() {
		timetableLoader =
			TimetableLoader(WeakReference(this), this, profileLink)
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
			setupTimetableLoader()
			showPersonalTimetable()

			recreate()
		} else {
			timetableLoader = null
		}
	}

	private fun refreshMessages(link: LinkDatabase.Link, navigationView: NavigationView) =
		GlobalScope.launch(Dispatchers.Main) {
			InfoCenterActivity.loadMessages(this@MainActivity, link)?.let {
				navigationView.menu.findItem(R.id.nav_infocenter).icon = if (
					it.size > preferences.defaultPrefs.getInt(
						"preference_last_messages_count",
						0
					) ||
					(SimpleDateFormat(
						"dd-MM-yyyy",
						Locale.US
					).format(Calendar.getInstance().time) != preferences.defaultPrefs.getString(
						"preference_last_messages_date",
						""
					)
							&& it.isNotEmpty())
				) {
					getDrawable(R.drawable.all_infocenter_dot)
				} else {
					getDrawable(R.drawable.all_infocenter)
				}
			}
		}

	private fun setupViews() {
		setupWeekView()
		restoreZoomLevel()

		textview_main_lastrefresh?.text =
			getString(R.string.main_last_refreshed, getString(R.string.main_last_refreshed_never))

		setupSwipeRefresh()
	}

	private fun setupSwipeRefresh() {
		swiperefreshlayout_main_timetable.setOnRefreshListener {
			loadTimetable(true)
		}
	}

	//TODO: fix items shown three times
	private fun loadTimetable(
		forceRefresh: Boolean = false
	) {
		if (timetableLoader == null) return

		weekView.notifyDataSetChanged()
		if (!forceRefresh) showLoading(true)

		val alwaysLoad = PreferenceUtils.getPrefBool(
			preferences,
			"preference_connectivity_refresh_in_background"
		)
		val flags =
			(if (!forceRefresh) TimetableLoader.FLAG_LOAD_CACHE else 0) or (if (alwaysLoad || forceRefresh) TimetableLoader.FLAG_LOAD_SERVER else 0)
		timetableLoader!!.load(flags)
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

	private fun setupWeekView() {
		weekView = findViewById(R.id.weekview_main_timetable)
		weekView.setOnEventClickListener(this)
		weekView.setOnCornerClickListener(this)
		weekView.setPeriodChangeListener(this)
		weekView.scrollListener = object : ScrollListener {
			override fun onFirstVisibleDayChanged(
				newFirstVisibleDay: LocalDate,
				oldFirstVisibleDay: LocalDate?
			) {
				setLastRefresh(
					weeklyTimetableItems?.lastUpdated
						?: 0
				)
			}
		}
		weekView.scaleListener = object : ScaleListener {
			override fun onScaleFinished() {
				saveZoomLevel()
			}
		}
		setupWeekViewConfig()
	}

	internal fun saveZoomLevel() {
		preferences.defaultPrefs.edit().apply {
			putInt(PERSISTENT_INT_ZOOM_LEVEL, weekView.hourHeight)
			apply()
		}
	}

	private fun restoreZoomLevel() {
		weekView.hourHeight =
			preferences.defaultPrefs.getInt(PERSISTENT_INT_ZOOM_LEVEL, weekView.hourHeight)
	}

	private fun setupWeekViewConfig() {
		weekView.weekLength = preferences.defaultPrefs.getStringSet(
			"preference_week_custom_range",
			emptySet()
		)?.size?.zeroToNull
			?: 6
		weekView.numberOfVisibleDays =
			preferences.defaultPrefs.getInt("preference_week_custom_display_length", 0).zeroToNull
				?: weekView.weekLength
		weekView.firstDayOfWeek =
			preferences.defaultPrefs.getStringSet("preference_week_custom_range", emptySet())
				?.map { MaterialDayPicker.Weekday.valueOf(it) }?.minOrNull()?.ordinal
				?: 1

		weekView.timeColumnVisibility =
			!PreferenceUtils.getPrefBool(preferences, "preference_timetable_hide_time_stamps")

		weekView.columnGap = ConversionUtils.dpToPx(
			PreferenceUtils.getPrefInt(
				preferences,
				"preference_timetable_item_padding"
			).toFloat(), this
		).toInt()
		weekView.overlappingEventGap = ConversionUtils.dpToPx(
			PreferenceUtils.getPrefInt(
				preferences,
				"preference_timetable_item_padding_overlap"
			).toFloat(), this
		).toInt()
		weekView.eventCornerRadius = ConversionUtils.dpToPx(
			PreferenceUtils.getPrefInt(
				preferences,
				"preference_timetable_item_corner_radius"
			).toFloat(), this
		).toInt()
		weekView.eventSecondaryTextCentered =
			PreferenceUtils.getPrefBool(preferences, "preference_timetable_centered_lesson_info")
		weekView.eventTextBold =
			PreferenceUtils.getPrefBool(preferences, "preference_timetable_bold_lesson_name")
		weekView.eventTextSize = ConversionUtils.spToPx(
			PreferenceUtils.getPrefInt(
				preferences,
				"preference_timetable_lesson_name_font_size"
			).toFloat(), this
		)
		weekView.eventSecondaryTextSize = ConversionUtils.spToPx(
			PreferenceUtils.getPrefInt(
				preferences,
				"preference_timetable_lesson_info_font_size"
			).toFloat(), this
		)
		weekView.eventTextColor = if (PreferenceUtils.getPrefBool(
				preferences,
				"preference_timetable_item_text_light"
			)
		) Color.WHITE else Color.BLACK
		weekView.pastBackgroundColor =
			PreferenceUtils.getPrefInt(preferences, "preference_background_past")
		weekView.futureBackgroundColor =
			PreferenceUtils.getPrefInt(preferences, "preference_background_future")
		weekView.nowLineColor = PreferenceUtils.getPrefInt(preferences, "preference_marker")

		weekView.horizontalFlingEnabled =
			PreferenceUtils.getPrefBool(preferences, "preference_fling_enable")
		weekView.snapToWeek = !PreferenceUtils.getPrefBool(
			preferences,
			"preference_week_snap_to_days"
		) && weekView.numberOfVisibleDays != 1
	}

	override fun onPeriodChange(
		startDate: LocalDate,
		endDate: LocalDate
	): List<WeekViewDisplayable<TimegridItem>> {
		return weeklyTimetableItems?.items ?: run {
			weeklyTimetableItems = WeeklyTimetableItems().apply {
				loadTimetable()
			}
			emptyList()
		}
	}

	private fun setupHours() {
		val lines = mutableListOf(480, 570, 590, 680, 690, 780, 840, 930, 940, 1030, 1040, 1130)
		val labels = mutableListOf("1", "2", "3", "4", "5", "6")
		val range = RangePreference.convertToPair(
			PreferenceUtils.getPrefString(
				preferences,
				"preference_timetable_range",
				null
			)
		)

		if (!PreferenceUtils.getPrefBool(preferences, "preference_timetable_range_index_reset"))
			weekView.hourIndexOffset = (range?.first ?: 1) - 1
		weekView.hourLines = lines.toIntArray()
		weekView.hourLabels = labels.toTypedArray().let { hourLabelArray ->
			if (hourLabelArray.joinToString("") == "") IntArray(
				labels.size,
				fun(idx: Int): Int { return idx + 1 }).map { it.toString() }.toTypedArray()
			else hourLabelArray
		}
		weekView.startTime = lines.first()
		weekView.endTime = lines.last() + 30 // TODO: Don't hard-code this offset
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

	private fun prepareItems(items: List<TimegridItem>): List<TimegridItem> {
		val newItems = items.mapNotNull { item ->
			if (PreferenceUtils.getPrefBool(
					preferences,
					"preference_timetable_hide_cancelled"
				) && item.period.type == Period.Type.CANCELLED
			) return@mapNotNull null
			item
		}
		colorItems(newItems)
		return newItems
	}

	private fun colorItems(items: List<TimegridItem>) {
		val regularColor = PreferenceUtils.getPrefInt(preferences, "preference_background_regular")
		val cancelledColor =
			PreferenceUtils.getPrefInt(preferences, "preference_background_cancelled")
		val irregularColor =
			PreferenceUtils.getPrefInt(preferences, "preference_background_irregular")

		val regularPastColor =
			PreferenceUtils.getPrefInt(preferences, "preference_background_regular_past")
		val cancelledPastColor =
			PreferenceUtils.getPrefInt(preferences, "preference_background_cancelled_past")
		val irregularPastColor =
			PreferenceUtils.getPrefInt(preferences, "preference_background_irregular_past")

		val useDefault =
			preferences.defaultPrefs.getStringSet("preference_school_background", emptySet())
				?: emptySet()
		val useTheme = if (!useDefault.contains("regular")) PreferenceUtils.getPrefBool(
			preferences,
			"preference_use_theme_background"
		) else false

		items.forEach { item ->
			item.color = when {
				item.period.type == Period.Type.CANCELLED -> cancelledColor
				item.period.type == Period.Type.IRREGULAR -> irregularColor
				useTheme -> getAttr(R.attr.colorPrimary)
				else -> regularColor
			}

			item.pastColor = when {
				item.period.type == Period.Type.CANCELLED -> cancelledPastColor
				item.period.type == Period.Type.IRREGULAR -> irregularPastColor
				useTheme -> if (currentTheme == "pixel") getAttr(R.attr.colorPrimary).darken(0.25f) else getAttr(
					R.attr.colorPrimaryDark
				)
				else -> regularPastColor
			}
		}
	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.nav_settings -> {
				val i = Intent(this, SettingsActivity::class.java)
				i.putExtra(SettingsActivity.EXTRA_LONG_PROFILE_ID, profileId)
				startActivityForResult(i, REQUEST_CODE_SETTINGS)
			}
			R.id.nav_infocenter -> {
				val i = Intent(this, InfoCenterActivity::class.java)
				i.putExtra(InfoCenterActivity.EXTRA_LONG_PROFILE_ID, profileId)
				startActivity(i)
			}
			R.id.nav_locations -> {
				startActivity(Intent(this, LocationActivity::class.java))
			}
			R.id.nav_mensa -> {
				startActivity(Intent(this, MensaActivity::class.java))
			}
		}

		closeDrawer()
		return true
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
		} else if (!showPersonalTimetable()) {
			if (System.currentTimeMillis() - 2000 > lastBackPress && PreferenceUtils.getPrefBool(
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
	}

	private fun setTarget(): Boolean {
		weeklyTimetableItems = null
		weekView.notifyDataSetChanged()
		return true
	}

	internal fun setFullscreenDialogActionBar() {
		supportActionBar?.setHomeAsUpIndicator(R.drawable.all_close)
		supportActionBar?.setTitle(R.string.all_lesson_details)
	}

	internal fun setDefaultActionBar() {
		supportActionBar?.title = resources.getString(R.string.app_name)
	}

	override fun onEventClick(data: TimegridItem, eventRect: RectF) {
		viewModelStore.clear() // TODO: Doesn't seem like the best solution. This could potentially interfere with other ViewModels scoped to this activity.
		val fragment = TimetableItemDetailsFragment(data)

		supportFragmentManager.beginTransaction().run {
			setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
			add(R.id.content_main, fragment, FRAGMENT_TAG_LESSON_INFO)
			addToBackStack(fragment.tag)
			commit()
		}
	}

	internal fun setLastRefresh(timestamp: Long) {
		textview_main_lastrefresh?.text = if (timestamp > 0L)
			getString(
				R.string.main_last_refreshed,
				formatTimeDiff(Instant.now().millis - timestamp)
			)
		else
			getString(R.string.main_last_refreshed, getString(R.string.main_last_refreshed_never))
	}

	private fun formatTimeDiff(diff: Long): String {
		return when {
			diff < MINUTE_MILLIS -> getString(R.string.main_time_diff_just_now)
			diff < HOUR_MILLIS -> resources.getQuantityString(
				R.plurals.main_time_diff_minutes,
				((diff / MINUTE_MILLIS).toInt()),
				diff / MINUTE_MILLIS
			)
			diff < DAY_MILLIS -> resources.getQuantityString(
				R.plurals.main_time_diff_hours,
				((diff / HOUR_MILLIS).toInt()),
				diff / HOUR_MILLIS
			)
			else -> resources.getQuantityString(
				R.plurals.main_time_diff_days,
				((diff / DAY_MILLIS).toInt()),
				diff / DAY_MILLIS
			)
		}
	}

	override fun addTimetableItems(
		items: List<TimegridItem>,
		timestamp: Long
	) {
		weeklyTimetableItems?.apply {
			this.items = prepareItems(items).map { it.toWeekViewEvent() }
			lastUpdated = timestamp
		}
		weekView.notifyDataSetChanged()

		// TODO: Only disable these loading indicators when everything finished loading
		showLoading(false)
	}

	override fun onTimetableLoadingError(requestId: Int, code: Int?, message: String?) {
		if (timetableLoader == null) return

		when (code) {
			TimetableLoader.CODE_CACHE_MISSING -> timetableLoader!!.repeat(
				requestId,
				TimetableLoader.FLAG_LOAD_SERVER
			)
			else -> {
				showLoading(false)
				Snackbar.make(
					content_main,
					if (code != null) ErrorMessageDictionary.getErrorMessage(
						resources,
						code
					) else message
						?: getString(R.string.all_error),
					Snackbar.LENGTH_INDEFINITE
				)
					.setAction("Show") {
						ErrorReportingDialog(this).showRequestErrorDialog(
							requestId,
							code,
							message
						)
					}
					.show()
			}
		}
	}

	private fun showLoading(loading: Boolean) {
		if (!loading) swiperefreshlayout_main_timetable.isRefreshing = false
		progressbar_main_loading?.visibility = if (loading) View.VISIBLE else View.GONE
	}

	override fun onCornerClick() {
		val fragment = DatePickerDialog()

		lastPickedDate?.let {
			val args = Bundle()
			args.putInt("year", it.year)
			args.putInt("month", it.monthOfYear)
			args.putInt("day", it.dayOfMonth)
			fragment.arguments = args
		}
		fragment.dateSetListener =
			android.app.DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
				DateTime().withDate(year, month + 1, dayOfMonth).let {
					// +1 compensates for conversion from Calendar to DateTime
					weekView.goToDate(it)
					lastPickedDate = it
				}
			}
		fragment.show(supportFragmentManager, "datePicker")
	}

	override fun onCornerLongClick() = weekView.goToToday()

	private fun openDrawer(drawer: DrawerLayout = drawer_layout) =
		drawer.openDrawer(GravityCompat.START)

	private fun closeDrawer(drawer: DrawerLayout = drawer_layout) =
		drawer.closeDrawer(GravityCompat.START)

	private fun Int.darken(ratio: Float) = ColorUtils.blendARGB(this, Color.BLACK, ratio)

	internal class WeeklyTimetableItems {
		var items: List<WeekViewEvent<TimegridItem>> = emptyList()
		var lastUpdated: Long = 0
	}
}

private val Int.zeroToNull: Int?
	get() = if (this != 0) this else null
