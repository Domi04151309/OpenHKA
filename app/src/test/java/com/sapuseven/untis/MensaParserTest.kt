package com.sapuseven.untis

import com.sapuseven.untis.fragments.MensaFragment
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MensaParserTest {

	@Test
	fun parseAdditives() {
		val file = Helpers.getFileContents("/iwii/REST/canteen/v2/foodadditives.json")
		val result = MensaFragment.parseAdditives(file)

		var num = 0
		Assert.assertEquals("1: mit Farbstoff", result[num])

		num = 1
		Assert.assertEquals("2: mit Konservierungsstoff", result[num])
	}

	@Test
	fun parseCanteens() {
		val file = Helpers.getFileContents("/hskampus-broker/api/canteen.json")
		val result = MensaFragment.parseCanteens(file)

		var num = 0
		Assert.assertEquals("Adenauerring", result.list[num])

		num = 1
		Assert.assertEquals("Moltke", result.list[num])
	}

	@Test
	fun parseMenu() {
		val file = Helpers.getFileContents("/hskampus-broker/api/canteen/2/date/2023-01-25.json")

		val result = MensaFragment.parseMenu(
			RuntimeEnvironment.getApplication().applicationContext.resources,
			file,
			"Student"
		)

		var num = 0
		Assert.assertEquals("", result.list[num].title)
		Assert.assertEquals("Wahlessen 1", result.list[num].summary)
		Assert.assertEquals(null, result.list[num].price)

		num = 1
		Assert.assertEquals("Schnitzel Bar Puten und Schweineschnitzel je 100 g in Selbstbedienung", result.list[num].title)
		Assert.assertEquals("Additives: Ge, We", result.list[num].summary)
		Assert.assertEquals(1.05, result.list[num].price)
	}
}
