package com.sapuseven.untis.data

data class GenericParseResult<T, V>(
	val list: ArrayList<T> = arrayListOf(),
	val map: MutableMap<String, V> = mutableMapOf()
)
