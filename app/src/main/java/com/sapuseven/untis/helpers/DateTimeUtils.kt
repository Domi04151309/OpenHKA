package com.sapuseven.untis.helpers

import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder

object DateTimeUtils {
	fun shortDisplayableTime(): DateTimeFormatter {
		return Constants.sdt
	}

	object Constants {
		internal val sdt = DateTimeFormatterBuilder()
				.appendHourOfDay(1)
				.appendLiteral(':')
				.appendMinuteOfHour(2)
				.toFormatter()
	}
}
