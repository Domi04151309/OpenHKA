package com.sapuseven.untis.data.lists

data class StationItem(
	val title: String,
	var departures: Array<DepartureListItem>
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as StationItem

		if (title != other.title) return false
		if (!departures.contentEquals(other.departures)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = title.hashCode()
		result = 31 * result + departures.contentHashCode()
		return result
	}
}
