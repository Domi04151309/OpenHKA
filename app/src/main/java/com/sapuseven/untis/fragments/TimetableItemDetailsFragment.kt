package com.sapuseven.untis.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.MainActivity
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.helpers.ConversionUtils
import com.sapuseven.untis.models.untis.timetable.Period
import com.sapuseven.untis.viewmodels.PeriodDataViewModel
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

class TimetableItemDetailsFragment(item: TimegridItem?) : Fragment() {
	constructor() : this(null)

	private val viewModel: PeriodDataViewModel by activityViewModels {
		PeriodDataViewModel.Factory(
			item,
		)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return activity?.let { activity ->
			viewModel.item.period.let {
				generateView(activity, container, it)
			}
		} ?: throw IllegalStateException("Activity cannot be null")
	}

	override fun onStart() {
		super.onStart()
		if (activity is MainActivity) (activity as MainActivity).setFullscreenDialogActionBar()
	}

	override fun onStop() {
		super.onStop()
		if (activity is MainActivity) (activity as MainActivity).setDefaultActionBar()
	}

	private fun generateView(
		activity: FragmentActivity,
		container: ViewGroup?,
		period: Period
	): View {
		val root = activity.layoutInflater.inflate(
			R.layout.fragment_timetable_item_details_page,
			container,
			false
		) as ScrollView
		val linearLayout = root.getChildAt(0) as LinearLayout

		val attrs = intArrayOf(android.R.attr.textColorPrimary)
		val ta = context?.obtainStyledAttributes(attrs)
		val color = ta?.getColor(0, 0)
		ta?.recycle()

		populateList(linearLayout.findViewById(R.id.llRoomList), period.location, color)

		linearLayout.findViewById<LinearLayout>(R.id.llInfo).visibility =
			if (period.hasIndicator) View.VISIBLE else View.GONE
		if (period.hasIndicator) {
			populateList(linearLayout.findViewById(R.id.llInfoList), period.info, color)
		}

		var title = period.title
		if (period.type == Period.Type.CANCELLED)
			title = getString(R.string.all_lesson_cancelled, period.title)
		else if (period.type == Period.Type.IRREGULAR)
			title = getString(R.string.all_lesson_irregular, period.title)

		(linearLayout.findViewById(R.id.title) as TextView).text = title

		linearLayout.findViewById<TextView>(R.id.time).text =
			formatLessonTime(period.startDate.toLocalDateTime(), period.endDate.toLocalDateTime())

		return root
	}

	private fun populateList(
		list: LinearLayout,
		text: String,
		textColor: Int?
	): Boolean {
		generateTextViewForElement(
			text,
			textColor
		)?.let { list.addView(it) }
		return false
	}

	private fun generateTextViewForElement(
		text: String,
		textColor: Int?
	): TextView? {
		val tv = TextView(requireContext())
		val params = LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		)
		params.setMargins(0, 0, ConversionUtils.dpToPx(12.0f, requireContext()).toInt(), 0)
		tv.text = text
		if (tv.text.isBlank()) return null
		tv.layoutParams = params
		textColor?.let { tv.setTextColor(it) }
		tv.gravity = Gravity.CENTER_VERTICAL
		return tv
	}

	private fun formatLessonTime(startDateTime: LocalDateTime, endDateTime: LocalDateTime): String {
		return requireContext().getString(
			R.string.main_dialog_itemdetails_timeformat,
			startDateTime.toString(DateTimeFormat.shortTime()),
			endDateTime.toString(DateTimeFormat.shortTime())
		)
	}
}
