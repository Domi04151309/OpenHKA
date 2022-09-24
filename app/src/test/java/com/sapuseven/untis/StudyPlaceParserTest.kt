package com.sapuseven.untis

import com.sapuseven.untis.fragments.StudyPlaceFragment
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StudyPlaceParserTest {

	@Test
	fun parseStudyPlaces() {
		val places = Helpers.getFileContents("/hskampus-broker/api/learningPlaces.json")
		val occupations =
			Helpers.getFileContents("/hskampus-broker/api/learningPlaceOccupations.json")

		val result = StudyPlaceFragment.parseStudyPlaces(
			RuntimeEnvironment.getApplication().applicationContext.resources,
			places,
			occupations
		)

		var num = 0
		Assert.assertEquals(0, result.list[num].value)
		Assert.assertEquals(102, result.list[num].max)
		Assert.assertEquals("0 / 102 occupied", result.list[num].overline)
		Assert.assertEquals("Badische Landesbibliothek,\nHauptgebäude", result.list[num].title)
		Assert.assertEquals("Mo - Fr: 09:00-19:00 Uhr, Sa: 10:00-18:00 Uhr", result.list[num].summary)

		num = 1
		Assert.assertEquals(12, result.list[num].value)
		Assert.assertEquals(21, result.list[num].max)
		Assert.assertEquals("12 / 21 occupied", result.list[num].overline)
		Assert.assertEquals("Badische Landesbibliothek,\nWissenstor", result.list[num].title)
		Assert.assertEquals("Mo - Fr: 09:00-22:00 Uhr, Sa + So: 10:00-22:00 Uhr", result.list[num].summary)
	}
}
