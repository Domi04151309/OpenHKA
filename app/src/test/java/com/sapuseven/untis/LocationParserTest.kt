package com.sapuseven.untis

import com.sapuseven.untis.fragments.LocationFragment
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocationParserTest {

    @Test
    fun parseLocations() {
		val file = Helpers.getFileContents("/iwii/REST/buildings/v2/all.json")
		val result = LocationFragment.parseLocations(
			RuntimeEnvironment.getApplication().applicationContext.resources,
			file
		)

		var num = 0
		Assert.assertEquals("", result.list[num].title)
		Assert.assertEquals("Building A", result.list[num].summary)

		num = 1
		Assert.assertEquals("Allgemeiner Studierendenausschuss", result.list[num].title)
		Assert.assertEquals("", result.list[num].summary)
    }
}
