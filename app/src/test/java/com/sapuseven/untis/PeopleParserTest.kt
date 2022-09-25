package com.sapuseven.untis

import com.sapuseven.untis.fragments.PeopleFragment
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PeopleParserTest {

    @Test
    fun parsePeople() {
		val file = Helpers.getFileContents("/hskampus-broker/api/persons.json")
		val result = PeopleFragment.parsePeople(file)

		var num = 0
		Assert.assertEquals("https://www.h-ka.de/typo3temp/assets/_processed_/2/9/csm_profile_dummy_77848f19a4.png", result.list[num].pictureURL)
		Assert.assertEquals("", result.list[num].overline)
		Assert.assertEquals("Aberle, Marcus", result.list[num].title)
		Assert.assertEquals("marcus.aberle@h-ka.de | AB", result.list[num].summary)

		num = 1
		Assert.assertEquals("https://www.h-ka.de/fileadmin/_processed_/9/e/csm_0_01_83aa4fcf86.jpg", result.list[num].pictureURL)
		Assert.assertEquals("Dr.-Ing.", result.list[num].overline)
		Assert.assertEquals("Aboalam, Kawther", result.list[num].title)
		Assert.assertEquals("kawther.aboalam@h-ka.de | EIT", result.list[num].summary)
    }
}
