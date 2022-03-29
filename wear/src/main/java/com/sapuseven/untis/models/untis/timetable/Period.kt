package com.sapuseven.untis.models.untis.timetable

import net.fortuna.ical4j.model.Component
import org.joda.time.DateTime
import org.joda.time.DateTimeZone


class Period(component: Component, timeZone: DateTimeZone) {

	private val properties = component.properties
	private val unformattedTitle = properties.getProperty("SUMMARY").value.trim()

	val startDate: DateTime = stringToDate(properties.getProperty("DTSTART").value, timeZone)
	val endDate: DateTime = stringToDate(properties.getProperty("DTEND").value, timeZone)
	val title: String = formatTitle(unformattedTitle)
	val location: String = properties.getProperty("LOCATION").value
	val type: Type = when (properties.getProperty("CATEGORIES").value) {
		"AUSFALL" -> Type.CANCELLED
		"AKTUELL" -> Type.IRREGULAR
		else -> Type.REGULAR
	}
	val hasIndicator: Boolean = unformattedTitle.startsWith('*')
	val info: String = properties.getProperty("DESCRIPTION").value
		.split(' ').drop(4).joinToString(" ")

	enum class Type {
		REGULAR, IRREGULAR, CANCELLED
	}

	private fun formatTitle(title: String): String {
		var returnValue = title
		if (returnValue.startsWith('+')) returnValue = returnValue.substring(15).trim()
		if (returnValue.startsWith('*')) returnValue = returnValue.substring(1).trim()
		return returnValue
	}

	companion object {
		//TODO: will break in 10000 years
		fun stringToDate(string: String, timeZone: DateTimeZone): DateTime {
			val dateTime = DateTime(
				string.substring(0, 4).toInt(),
				string.substring(4, 6).toInt(),
				string.substring(6, 8).toInt(),
				string.substring(9, 11).toInt(),
				string.substring(11, 13).toInt(),
				string.substring(13, 15).toInt(),
				timeZone
			)
			return dateTime.plusMillis(timeZone.getOffset(dateTime.toInstant()))
		}
	}
}
