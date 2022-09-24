package com.sapuseven.untis.data

data class GenericParseResult<T, V>(
	var list: ArrayList<T> = arrayListOf(),
	var map: MutableMap<String, V> = mutableMapOf()
)
