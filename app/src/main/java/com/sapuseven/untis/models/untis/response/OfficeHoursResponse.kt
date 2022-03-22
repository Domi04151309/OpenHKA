package com.sapuseven.untis.models.untis.response

import com.sapuseven.untis.models.UntisOfficeHour
import kotlinx.serialization.Serializable

@Serializable
data class OfficeHoursResult(
		val officeHours: List<UntisOfficeHour>
)
