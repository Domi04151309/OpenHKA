package com.sapuseven.untis.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import com.prof.rssparser.Article
import com.prof.rssparser.Parser
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.BaseActivity
import com.sapuseven.untis.activities.MainActivity
import com.sapuseven.untis.adapters.infocenter.*
import com.sapuseven.untis.data.databases.LinkDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class InfoCenterFragment : Fragment() {
	private val messageList = arrayListOf<Article>()
	private val messageAdapter = RSSAdapter(messageList)
	private var link: LinkDatabase.Link? = null
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		//TODO: find out faster if it is json
		suspend fun loadMessages(context: Context, link: LinkDatabase.Link): List<Article>? {
			val parser = Parser.Builder()
				.context(context)
				.charset(StandardCharsets.UTF_8)
				.cacheExpirationMillis(24L * 60L * 60L * 100L) // one day
				.build()

			return try {
				parser.getChannel(link.rssUrl).articles
			} catch (e: Exception) {
				link.rssUrl.httpGet().awaitStringResult()
					.fold({ data ->
						//TODO: add cache support
						val json = JSONArray(data)
						val articles = arrayListOf<Article>()
						var currentItem: JSONObject
						for (i in 0 until json.length()) {
							currentItem = json.getJSONObject(i)
							articles.add(
								Article(
									"",
									currentItem.optString("title"),
									"",
									"",
									currentItem.optString("updatedAt"),
									currentItem.optString("content"),
									"",
									"",
									"",
									"",
									"",
									"",
									listOf(),
									null
								)
							)
						}
						articles
					}, {
						//TODO: handle error
						null
					})
			}
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

		link = (activity as MainActivity).profileLink
		link?.let {
			refreshMessages(it)
		}
		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = messageAdapter
		swiperefreshlayout.setOnRefreshListener { link?.let { refreshMessages(it) } }

		return root
	}

	private fun refreshMessages(link: LinkDatabase.Link) = GlobalScope.launch(Dispatchers.Main) {
		swiperefreshlayout.isRefreshing = true
		loadMessages(requireContext(), link)?.let {
			messageList.clear()
			messageList.addAll(it)
			messageAdapter.notifyDataSetChanged()

			(activity as MainActivity).setInfoCenterDot(false)

			if (it.isNotEmpty()) {
				(activity as BaseActivity).preferences.defaultPrefs.edit()
					.putString("preference_last_title", it[0].title)
					.apply()
			}
		}
		swiperefreshlayout.isRefreshing = false
	}
}
