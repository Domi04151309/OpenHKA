package com.sapuseven.untis.wear.activities

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.support.wearable.input.RotaryEncoder
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import androidx.preference.PreferenceManager
import com.sapuseven.untis.R
import com.sapuseven.untis.data.databases.LinkDatabase
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.helpers.timetable.TimetableLoader
import com.sapuseven.untis.interfaces.TimetableDisplay
import com.sapuseven.untis.models.untis.timetable.Period
import com.sapuseven.untis.wear.adapters.TimetableListAdapter
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.lang.ref.WeakReference
import kotlin.math.roundToInt


class MainActivity : WearableActivity(), TimetableDisplay {

	private var scrollView: ScrollView? = null
	private var preferences: com.sapuseven.untis.helpers.config.PreferenceManager? = null
	private var timetableListAdapter: TimetableListAdapter? = null
	private val linkDatabase = LinkDatabase.createInstance(this)
	private var profileId: Long = -1

	private lateinit var profileLink: LinkDatabase.Link
	private lateinit var timetableLoader: TimetableLoader

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		adjustInset(findViewById(R.id.content))
		scrollView = findViewById(R.id.root)
		preferences = com.sapuseven.untis.helpers.config.PreferenceManager(this)
		timetableListAdapter = TimetableListAdapter(this, findViewById(R.id.timetable))

		loadProfile()
		timetableLoader =
			TimetableLoader(WeakReference(this), this, profileLink)

		findViewById<Button>(R.id.reload).setOnClickListener {
			timetableListAdapter?.resetListLoading()
			loadTimetable(true)
		}

		findViewById<Button>(R.id.sign_out).setOnClickListener {
			PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putBoolean("signed_in", false).apply()
			startActivity(Intent(this, LoginActivity::class.java))
			finish()
		}
	}

	override fun onResume() {
		super.onResume()
		loadTimetable()
	}

	private fun loadProfile(): Boolean {
		if (linkDatabase.getLinkCount() < 1)
			return false

		profileId = preferences!!.currentProfileId()
		if (profileId == 0L || linkDatabase.getLink(profileId) == null) profileId =
			linkDatabase.getAllLinks()[0].id
				?: 0 // Fall back to the first user if an invalid user id is saved
		if (profileId == 0L) return false // No user found in database
		profileLink = linkDatabase.getLink(profileId) ?: return false

		preferences!!.saveProfileId(profileId)
		return true
	}

	private fun loadTimetable(force: Boolean = false) {
		timetableLoader.load(if (force) TimetableLoader.FLAG_LOAD_SERVER else TimetableLoader.FLAG_LOAD_CACHE)
	}

	override fun addTimetableItems(items: List<TimegridItem>, timestamp: Long) {
		val fmt: DateTimeFormatter = DateTimeFormat.forPattern("HH:mm")
		var time: String
		var title: String
		var room: String
		var text: String
		timetableListAdapter?.clearList()
		items.filter {
			val dateTime = DateTime(timestamp)
			it.startTime.dayOfYear() == dateTime.dayOfYear() && it.startTime.year() == dateTime.year()
		}.sortedBy {
			it.startTime
		}.forEach {
			time = it.period.startDate.toString(fmt) + " - " + it.period.endDate.toString(fmt)
			title = it.period.title
			room = it.period.location

			text = "$time\n$title"
			if (room != "") text += ", $room"
			timetableListAdapter?.addItem(text, it.period.type == Period.Type.CANCELLED)
		}
	}

	override fun onTimetableLoadingError(requestId: Int, code: Int?, message: String?) {
		Log.d("Timetable", message ?: "")
		when (code) {
			TimetableLoader.CODE_CACHE_MISSING -> timetableLoader.repeat(
				requestId,
				TimetableLoader.FLAG_LOAD_SERVER
			)
			else -> {
				timetableListAdapter?.resetListUnavailable()
			}
		}
	}

	override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
		if (event!!.action == MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(event)) {
			val delta =
				-RotaryEncoder.getRotaryAxisValue(event) * RotaryEncoder.getScaledScrollFactor(this)
			scrollView!!.scrollBy(0, delta.roundToInt())
			return true
		}
		return super.onGenericMotionEvent(event)
	}

	private fun adjustInset(layout: View) {
		if (applicationContext.resources.configuration.isScreenRound) {
			val inset = (FACTOR * Resources.getSystem().displayMetrics.widthPixels).toInt()
			layout.setPadding(inset, inset, inset, inset)
		}
	}

	companion object {
		private const val FACTOR = 0.146467f
	}
}
