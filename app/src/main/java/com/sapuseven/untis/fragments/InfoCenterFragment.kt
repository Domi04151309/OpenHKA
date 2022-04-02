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
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class InfoCenterFragment : Fragment() {
	private val messageList = arrayListOf<Article>()
	private val messageAdapter = MessageAdapter(messageList)
	private var messagesLoading = true
	private var link: LinkDatabase.Link? = null
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		suspend fun loadMessages(context: Context, link: LinkDatabase.Link): List<Article>? {
			val parser = Parser.Builder()
				.context(context)
				.charset(StandardCharsets.UTF_8)
				.cacheExpirationMillis(24L * 60L * 60L * 100L) // one day
				.build()

			return try {
				parser.getChannel(link.rssUrl).articles
			} catch (e: Exception) {
				null
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
		swiperefreshlayout.isRefreshing = messagesLoading
		swiperefreshlayout.setOnRefreshListener { link?.let { refreshMessages(it) } }

		return root
	}

	private fun refreshMessages(link: LinkDatabase.Link) = GlobalScope.launch(Dispatchers.Main) {
		messagesLoading = true
		loadMessages(requireContext(), link)?.let {
			messageList.clear()
			messageList.addAll(it)
			messageAdapter.notifyDataSetChanged()

			(activity as BaseActivity).preferences.defaultPrefs.edit()
				.putInt("preference_last_messages_count", it.size)
				.putString(
					"preference_last_messages_date",
					SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Calendar.getInstance().time)
				)
				.apply()
		}
		messagesLoading = false
		swiperefreshlayout.isRefreshing = false
	}
}
