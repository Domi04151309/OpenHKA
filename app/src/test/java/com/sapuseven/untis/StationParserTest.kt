package com.sapuseven.untis

import com.sapuseven.untis.activities.AddStationActivity
import com.sapuseven.untis.fragments.StationsFragment
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StationParserTest {

	@Test
	fun parseStation() {
		val file = Helpers.getFileContents("/kvv/XSLT_DM_REQUEST.json")

		val result = StationsFragment.parseStation(
			RuntimeEnvironment.getApplication().applicationContext.resources,
			file
		)

		Assert.assertEquals("Karlsruhe, Kunstakademie/Hochschule", result.first.title)
		Assert.assertEquals(
			"1 Heide in 11 min\n1 Durlach in 16 min\n1 Heide in 31 min\n1 Durlach in 36 min\n1 Heide in 51 min",
			result.first.summary
		)
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
