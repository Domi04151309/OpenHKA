package com.sapuseven.untis

import com.sapuseven.untis.activities.AddStationActivity
import com.sapuseven.untis.fragments.StationsFragment
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StationParserTest {

	@Test
	fun parseStation() {
		val file = Helpers.getFileContents("/kvv/XSLT_DM_REQUEST.json")
		val result = StationsFragment.parseStation(file)

		Assert.assertEquals("Karlsruhe, Kunstakademie/Hochschule", result.first.title)
		Assert.assertEquals("1, 1, 1, 1, 1, 1, 1, 1, 1, 1", result.first.summary)
	}

	@Test
	fun parseStations() {
		val file = Helpers.getFileContents("/kvv/XSLT_STOPFINDER_REQUEST.json")
		val result = AddStationActivity.parseStations(file, mutableSetOf())

		var num = 0
		Assert.assertEquals("Europaplatz/Postgalerie", result.list[num].title)
		Assert.assertEquals("Karlsruhe", result.list[num].summary)

		num = 1
		Assert.assertEquals("Europaplatz/Postgalerie (U)", result.list[num].title)
		Assert.assertEquals("Karlsruhe", result.list[num].summary)
	}
}
