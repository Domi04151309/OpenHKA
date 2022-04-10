package com.sapuseven.untis.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.MainActivity
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.models.untis.timetable.Period
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

class TimetableItemDetailsFragment(private val item: TimegridItem) : Fragment() {

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val root = inflater.inflate(
			R.layout.fragment_timetable_item_details_page,
			container,
			false
		)

		root.findViewById<TextView>(R.id.tvRooms).text = item.period.location

		val tvInfo = root.findViewById<TextView>(R.id.tvInfo)
		if (item.period.hasIndicator) {
			tvInfo.visibility = View.VISIBLE
			tvInfo.text = item.period.info
		} else {
			tvInfo.visibility = View.GONE
		}

		root.findViewById<TextView>(R.id.title).text =
			when (item.period.type) {
				Period.Type.CANCELLED -> getString(R.string.all_lesson_cancelled, item.period.title)
				Period.Type.IRREGULAR -> getString(R.string.all_lesson_irregular, item.period.title)
				else -> item.period.title
			}

		root.findViewById<TextView>(R.id.time).text = formatLessonTime(
			item.period.startDate.toLocalDateTime(), item.period.endDate.toLocalDateTime()
		)

		return root
	}

	override fun onStart() {
		super.onStart()
		if (activity is MainActivity) (activity as MainActivity).setFullscreenDialogActionBar(R.string.all_lesson_details)
	}

	override fun onStop() {
		super.onStop()
		if (activity is MainActivity) (activity as MainActivity).setDefaultActionBar()
	}

	private fun formatLessonTime(startDateTime: LocalDateTime, endDateTime: LocalDateTime): String {
		return requireContext().getString(
			R.string.main_dialog_itemdetails_timeformat,
			startDateTime.toString(DateTimeFormat.shortTime()),
			endDateTime.toString(DateTimeFormat.shortTime())
		)
	}
}
