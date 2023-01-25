package com.sapuseven.untis.data.lists

import android.content.res.Resources
import com.sapuseven.untis.R

data class MensaPricing(
	val student: Double,
	val guest: Double,
	val employee: Double,
	val pupil: Double
) {
	fun getPriceFromLevel(resources: Resources, level: String): Double {
		val values = resources.getStringArray(R.array.mensa_pricing_values)
		return when (values.indexOf(level)) {
			0 -> student
			1 -> guest
			2 -> employee
			3 -> pupil
			else -> 0.0
		}
	}
}
