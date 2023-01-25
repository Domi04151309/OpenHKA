package com.sapuseven.untis.data.lists

data class MensaListItem(
	val title: String,
	var summary: String,
	var price: Double?,
	var icon: Int? = null
)
