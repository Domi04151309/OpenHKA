package com.sapuseven.untis.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.BaseActivity
import com.sapuseven.untis.adapters.MessageAdapter
import com.sapuseven.untis.data.GenericParseResult
import com.sapuseven.untis.data.lists.ListItem
import com.sapuseven.untis.helpers.AuthenticationHelper
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.helpers.strings.StringLoaderAuth
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*


class GradesFragment : Fragment(), StringDisplay {
	private val adapter = MessageAdapter()
	private var parsedData: GenericParseResult<ListItem, Pair<String, String>> =
		GenericParseResult()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"

		fun parseGrades(input: String): GenericParseResult<ListItem, Pair<String, String>> {
			val result = GenericParseResult<ListItem, Pair<String, String>>()
			val json = JSONArray(input)
			val semesterMap = TreeMap<Int, Pair<String, TreeMap<String, ListItem>>>()
			var currentDegree: JSONArray
			var currentExam: JSONObject
			var currentExamName: String
			var currentSemester: String
			var currentSemesterId: Int
			for (i in 0 until json.length()) {
				currentDegree = json.getJSONObject(i).optJSONArray("grades") ?: JSONArray()
				for (j in 0 until currentDegree.length()) {
					currentExam = currentDegree.getJSONObject(j)
					currentExamName = currentExam.optString("examName")
					currentSemester = currentExam.optString("examSemester")
					currentSemesterId = currentExam.optInt("idExamSemester")
					if (!semesterMap.containsKey(currentSemesterId)) semesterMap[currentSemesterId] =
						Pair(currentSemester, TreeMap())
					semesterMap[currentSemesterId]?.second?.set(
						currentExamName, ListItem(
							currentExamName,
							currentExam.optDouble("grade").let {
								return@let if (it == 0.0) currentExam.optString("comment")
								else (it / 100).toString()
							}
						)
					)
					result.map[currentExamName] = Pair(
						currentExam.optString("examNumber"),
						currentSemester
					)
				}
			}
			for (key in semesterMap.descendingKeySet()) {
				result.list.add(
					ListItem(
						"",
						semesterMap[key]?.first ?: throw IllegalStateException()
					)
				)
				for ((_, exam) in semesterMap[key]?.second
					?: throw IllegalStateException()) result.list.add(exam)
			}
			return result
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(
			R.layout.fragment_infocenter,
			container,
			false
		)

		val auth = AuthenticationHelper((activity as BaseActivity).preferences)
		if (!auth.isLoggedIn()) {
			auth.loginDialog {
				onCreateView(inflater, container, savedInstanceState)
			}
			return null
		}

		stringLoader = StringLoaderAuth(
			WeakReference(context),
			this,
			"${API_URL}/grades/all",
			auth.get() ?: throw IllegalStateException()
		)
		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = adapter
		swiperefreshlayout.setOnRefreshListener { refreshGrades(StringLoader.FLAG_LOAD_SERVER) }

		refreshGrades(StringLoader.FLAG_LOAD_CACHE)

		adapter.onClickListener = View.OnClickListener {
			val key = it.findViewById<TextView>(R.id.textview_itemmessage_subject).text.toString()
			if (key.isNotEmpty()) {
				//TODO: add click behavior
				parsedData.map[key]
			}
		}

		return root
	}

	private fun refreshGrades(flags: Int) {
		swiperefreshlayout.isRefreshing = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		parsedData = parseGrades(string)
		adapter.updateItems(parsedData.list)
		swiperefreshlayout.isRefreshing = false
	}

	override fun onStringLoadingError(code: Int, loader: StringLoader) {
		when (code) {
			StringLoader.CODE_CACHE_MISSING -> loader.repeat(
				StringLoader.FLAG_LOAD_SERVER
			)
			else -> {
				Toast.makeText(
					context,
					R.string.errors_failed_loading_from_server_message,
					Toast.LENGTH_LONG
				).show()
				swiperefreshlayout.isRefreshing = false
			}
		}
	}
}
