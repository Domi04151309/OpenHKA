package com.sapuseven.untis.models.untis.response

import com.sapuseven.untis.models.UntisAbsence
import kotlinx.serialization.Serializable

@Serializable
data class AbsenceResult(
		val absences: List<UntisAbsence>
)
