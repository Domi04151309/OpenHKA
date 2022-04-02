package com.sapuseven.untis.fragments

import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ca.antonious.materialdaypicker.MaterialDayPicker
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.*
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.dialogs.DatePickerDialog
import com.sapuseven.untis.helpers.ConversionUtils
import com.sapuseven.untis.helpers.config.PreferenceUtils
import com.sapuseven.untis.helpers.timetable.TimetableLoader
import com.sapuseven.untis.interfaces.TimetableDisplay
import com.sapuseven.untis.models.untis.timetable.Period
import com.sapuseven.untis.preferences.RangePreference
import com.sapuseven.untis.views.weekview.WeekView
import com.sapuseven.untis.views.weekview.WeekViewDisplayable
import com.sapuseven.untis.views.weekview.WeekViewEvent
import com.sapuseven.untis.views.weekview.listeners.EventClickListener
import com.sapuseven.untis.views.weekview.listeners.ScaleListener
import com.sapuseven.untis.views.weekview.listeners.ScrollListener
import com.sapuseven.untis.views.weekview.listeners.TopLeftCornerClickListener
import com.sapuseven.untis.views.weekview.loaders.WeekViewLoader
import org.joda.time.DateTime
import org.joda.time.Instant
import org.joda.time.LocalDate
import java.lang.ref.WeakReference

class TimetableFragment : Fragment(),
	WeekViewLoader.PeriodChangeListener<TimegridItem>,
	EventClickListener<TimegridItem>,
	TopLeftCornerClickListener,
	TimetableDisplay {

	companion object {
		private const val MINUTE_MILLIS: Int = 60 * 1000
		private const val HOUR_MILLIS: Int = 60 * MINUTE_MILLIS
		private const val DAY_MILLIS: Int = 24 * HOUR_MILLIS

		private const val PERSISTENT_INT_ZOOM_LEVEL = "persistent_zoom_level"

		private const val FRAGMENT_TAG_LESSON_INFO = "com.sapuseven.untis.fragments.lessoninfo"
	}

	private var weeklyTimetableItems: WeeklyTimetableItems? = null
	private var lastPickedDate: DateTime? = null
	private val weekViewRefreshHandler = Handler(Looper.getMainLooper())
	private var timetableLoader: TimetableLoader? = null
	private lateinit var weekView: WeekView<TimegridItem>
	private lateinit var swipeRefreshLayout: SwipeRefreshLayout
	private lateinit var progressBar: ProgressBar
	private lateinit var lastRefreshed: TextView
	private lateinit var activity: MainActivity

	private val weekViewUpdate = object : Runnable {
		override fun run() {
			weekView.invalidate()
			weekViewRefreshHandler.postDelayed(this, 60 * 1000)
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(
			R.layout.fragment_timetable,
			container,
			false
		)
		swipeRefreshLayout = root.findViewById(R.id.swiperefreshlayout_main_timetable)
		progressBar = root.findViewById(R.id.progressbar_main_loading)
		lastRefreshed = root.findViewById(R.id.textview_main_lastrefresh)
		activity = getActivity() as MainActivity

		setupViews(root)
		setupHours()

		setupTimetableLoader()
		showPersonalTimetable()

		return root
	}

	override fun onPause() {
		weekViewRefreshHandler.removeCallbacks(weekViewUpdate)
		super.onPause()
	}

	override fun onResume() {
		super.onResume()
		if (timetableLoader == null) return
		if (::weekView.isInitialized) {
			setupWeekViewConfig()

			weekViewRefreshHandler.post(weekViewUpdate)
		}
	}

	private fun showPersonalTimetable(): Boolean {
		return setTarget()
	}

	private fun setupTimetableLoader() {
		timetableLoader =
			TimetableLoader(WeakReference(context), this, activity.profileLink)
	}

	private fun setupViews(root: View) {
		setupWeekView(root)
		restoreZoomLevel()

		lastRefreshed.text =
			getString(R.string.main_last_refreshed, getString(R.string.main_last_refreshed_never))

		setupSwipeRefresh()
	}

	private fun setupSwipeRefresh() {
		swipeRefreshLayout.setOnRefreshListener {
			loadTimetable(true)
		}
	}

	private fun loadTimetable(
		forceRefresh: Boolean = false
	) {
		if (timetableLoader == null) return

		weekView.notifyDataSetChanged()
		if (!forceRefresh) showLoading(true)

		val alwaysLoad = PreferenceUtils.getPrefBool(
			activity.preferences,
			"preference_connectivity_refresh_in_background"
		)
		val flags =
			(if (!forceRefresh) TimetableLoader.FLAG_LOAD_CACHE else 0) or (if (alwaysLoad || forceRefresh) TimetableLoader.FLAG_LOAD_SERVER else 0)
		timetableLoader!!.load(flags)
	}

	private fun setupWeekView(root: View) {
		weekView = root.findViewById(R.id.weekview_main_timetable)
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
		activity.preferences.defaultPrefs.edit().apply {
			putInt(PERSISTENT_INT_ZOOM_LEVEL, weekView.hourHeight)
			apply()
		}
	}

	private fun restoreZoomLevel() {
		weekView.hourHeight =
			activity.preferences.defaultPrefs.getInt(
				PERSISTENT_INT_ZOOM_LEVEL,
				weekView.hourHeight
			)
	}

	private fun setupWeekViewConfig() {
		weekView.weekLength = activity.preferences.defaultPrefs.getStringSet(
			"preference_week_custom_range",
			emptySet()
		)?.size?.zeroToNull
			?: 6
		weekView.numberOfVisibleDays =
			activity.preferences.defaultPrefs.getInt(
				"preference_week_custom_display_length",
				0
			).zeroToNull
				?: weekView.weekLength
		weekView.firstDayOfWeek =
			activity.preferences.defaultPrefs.getStringSet(
				"preference_week_custom_range",
				emptySet()
			)
				?.map { MaterialDayPicker.Weekday.valueOf(it) }?.minOrNull()?.ordinal
				?: 1

		weekView.timeColumnVisibility =
			!PreferenceUtils.getPrefBool(
				activity.preferences,
				"preference_timetable_hide_time_stamps"
			)

		weekView.columnGap = ConversionUtils.dpToPx(
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_timetable_item_padding"
			).toFloat(), requireContext()
		).toInt()
		weekView.overlappingEventGap = ConversionUtils.dpToPx(
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_timetable_item_padding_overlap"
			).toFloat(), requireContext()
		).toInt()
		weekView.eventCornerRadius = ConversionUtils.dpToPx(
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_timetable_item_corner_radius"
			).toFloat(), requireContext()
		).toInt()
		weekView.eventSecondaryTextCentered =
			PreferenceUtils.getPrefBool(
				activity.preferences,
				"preference_timetable_centered_lesson_info"
			)
		weekView.eventTextBold =
			PreferenceUtils.getPrefBool(
				activity.preferences,
				"preference_timetable_bold_lesson_name"
			)
		weekView.eventTextSize = ConversionUtils.spToPx(
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_timetable_lesson_name_font_size"
			).toFloat(), requireContext()
		)
		weekView.eventSecondaryTextSize = ConversionUtils.spToPx(
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_timetable_lesson_info_font_size"
			).toFloat(), requireContext()
		)
		weekView.eventTextColor = if (PreferenceUtils.getPrefBool(
				activity.preferences,
				"preference_timetable_item_text_light"
			)
		) Color.WHITE else Color.BLACK
		weekView.pastBackgroundColor =
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_background_past"
			)
		weekView.futureBackgroundColor =
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_background_future"
			)
		weekView.nowLineColor =
			PreferenceUtils.getPrefInt(activity.preferences, "preference_marker")

		weekView.horizontalFlingEnabled =
			PreferenceUtils.getPrefBool(
				activity.preferences,
				"preference_fling_enable"
			)
		weekView.snapToWeek = !PreferenceUtils.getPrefBool(
			activity.preferences,
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
				activity.preferences,
				"preference_timetable_range",
				null
			)
		)

		if (!PreferenceUtils.getPrefBool(
				activity.preferences,
				"preference_timetable_range_index_reset"
			)
		)
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

	private fun prepareItems(items: List<TimegridItem>): List<TimegridItem> {
		val newItems = items.mapNotNull { item ->
			if (PreferenceUtils.getPrefBool(
					activity.preferences,
					"preference_timetable_hide_cancelled"
				) && item.period.type == Period.Type.CANCELLED
			) return@mapNotNull null
			item
		}
		colorItems(newItems)
		return newItems
	}

	private fun colorItems(items: List<TimegridItem>) {
		val regularColor = PreferenceUtils.getPrefInt(
			activity.preferences,
			"preference_background_regular"
		)
		val cancelledColor =
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_background_cancelled"
			)
		val irregularColor =
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_background_irregular"
			)

		val regularPastColor =
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_background_regular_past"
			)
		val cancelledPastColor =
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_background_cancelled_past"
			)
		val irregularPastColor =
			PreferenceUtils.getPrefInt(
				activity.preferences,
				"preference_background_irregular_past"
			)

		val useDefault =
			activity.preferences.defaultPrefs.getStringSet(
				"preference_school_background",
				emptySet()
			)
				?: emptySet()
		val useTheme = if (!useDefault.contains("regular")) PreferenceUtils.getPrefBool(
			activity.preferences,
			"preference_use_theme_background"
		) else false

		items.forEach { item ->
			item.color = when {
				item.period.type == Period.Type.CANCELLED -> cancelledColor
				item.period.type == Period.Type.IRREGULAR -> irregularColor
				useTheme -> activity.getAttr(R.attr.colorPrimary)
				else -> regularColor
			}

			item.pastColor = when {
				item.period.type == Period.Type.CANCELLED -> cancelledPastColor
				item.period.type == Period.Type.IRREGULAR -> irregularPastColor
				useTheme -> if (activity.currentTheme == "pixel") activity.getAttr(
					R.attr.colorPrimary
				).darken(0.25f) else activity.getAttr(
					R.attr.colorPrimaryDark
				)
				else -> regularPastColor
			}
		}
	}

	private fun setTarget(): Boolean {
		weeklyTimetableItems = null
		weekView.notifyDataSetChanged()
		return true
	}

	override fun onEventClick(data: TimegridItem, eventRect: RectF) {
		viewModelStore.clear() // TODO: Doesn't seem like the best solution. This could potentially interfere with other ViewModels scoped to this activity.
		val fragment = TimetableItemDetailsFragment(data)

		(activity as AppCompatActivity).supportFragmentManager.beginTransaction().run {
			setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
			add(R.id.content_main, fragment, FRAGMENT_TAG_LESSON_INFO)
			addToBackStack(fragment.tag)
			commit()
		}
	}

	internal fun setLastRefresh(timestamp: Long) {
		lastRefreshed.text = if (timestamp > 0L)
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
				Toast.makeText(
					context,
					R.string.errors_failed_loading_from_server_message,
					Toast.LENGTH_LONG
				).show()
			}
		}
	}

	private fun showLoading(loading: Boolean) {
		if (!loading) swipeRefreshLayout.isRefreshing = false
		progressBar.visibility = if (loading) View.VISIBLE else View.GONE
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
		fragment.show((activity as AppCompatActivity).supportFragmentManager, "datePicker")
	}

	override fun onCornerLongClick() = weekView.goToToday()

	private fun Int.darken(ratio: Float) = ColorUtils.blendARGB(this, Color.BLACK, ratio)

	internal class WeeklyTimetableItems {
		var items: List<WeekViewEvent<TimegridItem>> = emptyList()
		var lastUpdated: Long = 0
	}
}

private val Int.zeroToNull: Int?
	get() = if (this != 0) this else null
