package com.sapuseven.untis.data.timetable

import com.sapuseven.untis.models.untis.timetable.Period
import java.io.Serializable

class PeriodData(
		var element: Period
) : Serializable {
	var forceIrregular = false

	companion object {
		// TODO: Convert to string resources
		const val ELEMENT_NAME_SEPARATOR = ", "
		const val ELEMENT_NAME_UNKNOWN = "?"
	}

	fun isCancelled(): Boolean = false

	fun isIrregular(): Boolean = false

	fun isExam(): Boolean = false
}
