package com.sapuseven.untis

import com.sapuseven.untis.fragments.InfoCenterFragment
import com.sapuseven.untis.fragments.LocationFragment
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedParserTest {

    @Test
    fun parseFeed() {
		val file = Helpers.getFileContents("/hskampus-broker/api/news.json")
		val result = InfoCenterFragment.parseJSONFeed(file)

		var num = 0
		Assert.assertEquals("über HOLZ und BAU und TRAGWERKE", result[num].title)
		Assert.assertEquals("Gordian Kley zu Gast an der Fakultät für Architektur und Bauwesen – mit im Gepäck? Ein Vortrag über einen Werkstoff, der zukunftsweisend und zugleich traditionell ist.", result[num].description)
		Assert.assertEquals("1652221073", result[num].pubDate)

		num = 1
		Assert.assertEquals("Preis für Digitalisierung im Bauvorhaben", result[num].title)
		Assert.assertEquals("Masterabsolvent im Bauingenieurwesen mit dem 2. Preis beim BIM Award für seine Thesis zur Digitalisierung im Straßenbau von Verkehrsminister Winfried Hermann ausgezeichnet", result[num].description)
		Assert.assertEquals("1652221073", result[num].pubDate)
    }
}
