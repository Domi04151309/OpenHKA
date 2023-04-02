package com.sapuseven.untis.data.timetable

import com.sapuseven.untis.models.untis.timetable.Period
import com.sapuseven.untis.views.weekview.WeekViewEvent

class TimegridItem(
	val period: Period,
) : WeekViewEvent<TimegridItem>(0, startTime = period.startDate, endTime = period.endDate) {

	init {
		title = period.title
		top = ""
		bottom = period.location
		hasIndicator = period.hasIndicator
	}

	override fun toWeekViewEvent(): WeekViewEvent<TimegridItem> {
		return WeekViewEvent(
			id,
			title,
			top,
			bottom,
			startTime,
			endTime,
			color,
			pastColor,
			textColor,
			this,
			hasIndicator
		)
	}
}
