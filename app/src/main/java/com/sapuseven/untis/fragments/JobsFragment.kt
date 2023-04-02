package com.sapuseven.untis.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.BaseActivity
import com.sapuseven.untis.adapters.JobAdapter
import com.sapuseven.untis.data.lists.JobItem
import com.sapuseven.untis.helpers.AuthenticationHelper
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.helpers.strings.StringLoaderAuth
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference


class JobsFragment : Fragment(), StringDisplay {
	private val adapter = JobAdapter()
	private var parsedData = arrayListOf<JobItem>()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
		private const val OFFER_TYPE = "internship" //TODO: replace this

		fun parseJobs(input: String): ArrayList<JobItem> {
			val result = arrayListOf<JobItem>()
			val json = JSONObject(input).optJSONArray("offers") ?: JSONArray()
			var currentOffer: JSONObject
			for (i in 0 until json.length()) {
				currentOffer = json.getJSONObject(i)
				result.add(
					JobItem(
						(currentOffer.optJSONObject("company")
							?: JSONObject()).optString("companyName"),
						currentOffer.optString("shortDescription"),
						currentOffer.optString("description")
					)
				)
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
			R.layout.fragemnt_jobs,
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
			"${API_URL}/joboffer/v2/offers/$OFFER_TYPE/0/-1",
			auth.get() ?: throw IllegalStateException()
		)
		recyclerview = root.findViewById(R.id.recyclerview_jobs)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_jobs)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = adapter
		swiperefreshlayout.setOnRefreshListener { refreshJobs(StringLoader.FLAG_LOAD_SERVER) }

		refreshJobs(StringLoader.FLAG_LOAD_CACHE)

		return root
	}

	private fun refreshJobs(flags: Int) {
		swiperefreshlayout.isRefreshing = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		parsedData = parseJobs(string)
		adapter.updateItems(parsedData)
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
