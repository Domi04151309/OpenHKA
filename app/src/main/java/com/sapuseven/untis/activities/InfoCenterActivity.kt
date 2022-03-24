package com.sapuseven.untis.activities

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.prof.rssparser.Article
import com.prof.rssparser.Parser
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.infocenter.*
import com.sapuseven.untis.data.databases.LinkDatabase
import kotlinx.android.synthetic.main.activity_infocenter.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class InfoCenterActivity : BaseActivity() {
	private val messageList = arrayListOf<Article>()

	private val messageAdapter = MessageAdapter(messageList)

	private var messagesLoading = true

	private lateinit var linkDatabase: LinkDatabase
	private var link: LinkDatabase.Link? = null

	companion object {
		const val EXTRA_LONG_PROFILE_ID = "com.sapuseven.untis.activities.profileid"

		suspend fun loadMessages(context: Context, link: LinkDatabase.Link): List<Article>? {
			val parser = Parser.Builder()
				.context(context)
				.charset(Charset.forName("ISO-8859-7"))
				.cacheExpirationMillis(24L * 60L * 60L * 100L) // one day
				.build()

			return try {
				parser.getChannel(link.rssUrl).articles
			} catch (e: Exception) {
				null
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_infocenter)

		linkDatabase = LinkDatabase.createInstance(this)
		link = linkDatabase.getLink(intent.getLongExtra(EXTRA_LONG_PROFILE_ID, -1))
		link?.let {
			refreshMessages(it)
		}

		recyclerview_infocenter.layoutManager = LinearLayoutManager(this)

		showList(
			messageAdapter,
			messagesLoading,
		) { user ->
			refreshMessages(user)
		}
	}

	private fun showList(
		adapter: RecyclerView.Adapter<*>,
		refreshing: Boolean,
		refreshFunction: (link: LinkDatabase.Link) -> Unit
	) {
		recyclerview_infocenter.adapter = adapter
		swiperefreshlayout_infocenter.isRefreshing = refreshing
		swiperefreshlayout_infocenter.setOnRefreshListener { link?.let { refreshFunction(it) } }
	}

	private fun refreshMessages(link: LinkDatabase.Link) = GlobalScope.launch(Dispatchers.Main) {
		messagesLoading = true
		loadMessages(this@InfoCenterActivity, link)?.let {
			messageList.clear()
			messageList.addAll(it)
			messageAdapter.notifyDataSetChanged()

			preferences.defaultPrefs.edit()
				.putInt("preference_last_messages_count", it.size)
				.putString(
					"preference_last_messages_date",
					SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Calendar.getInstance().time)
				)
				.apply()
		}
		messagesLoading = false
		swiperefreshlayout_infocenter.isRefreshing = false
	}
}
