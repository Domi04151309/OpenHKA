package com.sapuseven.untis.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.MessageAdapter
import com.sapuseven.untis.data.lists.ListItem
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference


class ICalLinkChooserActivity : BaseActivity(), StringDisplay {

	private val linkList = arrayListOf<ListItem>()
	private val linkAdapter = MessageAdapter(linkList)
	private val keyMap: MutableMap<String, String> = mutableMapOf()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/hskampus-broker/api"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.fragment_infocenter)
		supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)

		stringLoader =
			StringLoader(WeakReference(this), this, "${API_URL}/courses")
		recyclerview = findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(this)
		recyclerview.adapter = linkAdapter
		swiperefreshlayout.setOnRefreshListener { refreshEvents(StringLoader.FLAG_LOAD_SERVER) }

		refreshEvents(StringLoader.FLAG_LOAD_CACHE)

		linkAdapter.onClickListener = View.OnClickListener {
			val key = it.findViewById<TextView>(R.id.textview_itemmessage_subject).text.toString()
			if (key.isNotEmpty()) {
				setResult(RESULT_OK, Intent().putExtra("link", keyMap[key]))
				finish()
			}
		}
	}

	private fun refreshEvents(flags: Int) {
		swiperefreshlayout.isRefreshing = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		linkList.clear()
		val json = JSONArray(string)
		var currentCategory: JSONObject
		var semesters: JSONArray
		var currentSemester: JSONObject
		var mapKey: String
		for (i in 0 until json.length()) {
			currentCategory = json.getJSONObject(i)
			linkList.add(
				ListItem(
					"",
					currentCategory.optString("name")
				)
			)
			semesters = currentCategory.optJSONArray("semesters") ?: JSONArray()
			for (j in 0 until semesters.length()) {
				currentSemester = semesters.getJSONObject(j)
				if (currentSemester.optBoolean("isValidICal")) {
					mapKey = currentSemester.optString("name") +
							" (" + currentSemester.optString("id") + ")"
					linkList.add(ListItem(mapKey, ""))
					keyMap[mapKey] =
						"https://www.iwi.hs-karlsruhe.de/hskampus-broker/api/semesters/" +
								currentSemester.optString("id") + "/ical"
					//currentSemester.optString("iCalFileHttpLink")
				}
			}
		}
		linkAdapter.notifyDataSetChanged()
		swiperefreshlayout.isRefreshing = false
	}

	override fun onStringLoadingError(code: Int, loader: StringLoader) {
		when (code) {
			StringLoader.CODE_CACHE_MISSING -> loader.repeat(
				StringLoader.FLAG_LOAD_SERVER
			)
			else -> {
				Toast.makeText(
					this,
					R.string.errors_failed_loading_from_server_message,
					Toast.LENGTH_LONG
				).show()
				swiperefreshlayout.isRefreshing = false
			}
		}
	}
}
