package com.sapuseven.untis.models.untis.timetable

import net.fortuna.ical4j.model.Component
import org.joda.time.DateTime
import org.joda.time.DateTimeZone


class Period(component: Component, timeZone: DateTimeZone) {

	private val properties = component.properties

	var startDate: DateTime = stringToDate(properties.getProperty("DTSTART").value, timeZone)
	var endDate: DateTime = stringToDate(properties.getProperty("DTEND").value, timeZone)
	var title: String = properties.getProperty("SUMMARY").value.trim()
	var location: String = properties.getProperty("LOCATION").value
	var type: Type =
		if (properties.getProperty("CATEGORIES").value == "NORMAL") Type.REGULAR else Type.IRREGULAR

	enum class Type {
		REGULAR, IRREGULAR, CANCELLED
	}

	//TODO: will break in 1000 years
	//TODO: might break for daylight savings
	private fun stringToDate(string: String, timeZone: DateTimeZone): DateTime {
		return DateTime(
			string.substring(0, 4).toInt(),
			string.substring(4, 6).toInt(),
			string.substring(6, 8).toInt(),
			string.substring(9, 11).toInt() + 1, //this
			string.substring(11, 13).toInt(),
			string.substring(13, 15).toInt(),
			timeZone
		)
	}
}
