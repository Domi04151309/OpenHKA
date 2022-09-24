package com.sapuseven.untis.data

data class GenericParseResult<T, V>(
	val list: MutableList<T> = mutableListOf(),
	val map: MutableMap<String, V> = mutableMapOf()
)
