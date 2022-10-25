package com.sapuseven.untis.models.untis.timetable

import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.PropertyList
import org.joda.time.DateTime
import org.joda.time.DateTimeZone


class Period(component: Component, timeZone: DateTimeZone) {

	private val properties = component.properties
	private val unformattedTitle = properties.optProperty("SUMMARY").trim()

	val startDate: DateTime = stringToDate(properties.optProperty("DTSTART"), timeZone)
	val endDate: DateTime = stringToDate(properties.optProperty("DTEND"), timeZone)
	val title: String = formatTitle(unformattedTitle)
	val location: String = properties.optProperty("LOCATION")
	val type: Type = when (properties.optProperty("CATEGORIES")) {
		"AUSFALL", "VERLEGT" -> Type.CANCELLED
		"AKTUELL" -> Type.IRREGULAR
		else -> Type.REGULAR
	}
	val hasIndicator: Boolean = unformattedTitle.startsWith('*')
	val info: String = properties.optProperty("DESCRIPTION")
		.split(' ').drop(4).joinToString(" ")

	enum class Type {
		REGULAR, IRREGULAR, CANCELLED
	}

	private fun formatTitle(title: String): String {
		var returnValue = title
		if (returnValue.startsWith('+')) returnValue = returnValue.substring(15).trim()
		if (returnValue.startsWith('*')) returnValue = returnValue.substring(1).trim()
		return returnValue.replace("(online)", "")
			.replace("(präsenz)", "")
			.trim()
	}

	private fun PropertyList.optProperty(string: String): String = getProperty(string)?.value ?: ""

	companion object {
		//TODO: will break in 10000 years
		fun stringToDate(string: String, timeZone: DateTimeZone): DateTime {
			if (string.isEmpty()) return DateTime(0)
			val dateTime = if (string.contains('T')) {
				DateTime(
					string.substring(0, 4).toInt(),
					string.substring(4, 6).toInt(),
					string.substring(6, 8).toInt(),
					string.substring(9, 11).toInt(),
					string.substring(11, 13).toInt(),
					string.substring(13, 15).toInt(),
					timeZone
				)
			} else {
				DateTime(
					string.substring(0, 4).toInt(),
					string.substring(4, 6).toInt(),
					string.substring(6, 8).toInt(),
					0,
					0,
					timeZone
				)
			}
			return dateTime.plusMillis(timeZone.getOffset(dateTime.toInstant()))
		}
	}
}
