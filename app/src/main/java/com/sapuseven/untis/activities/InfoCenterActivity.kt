package com.sapuseven.untis.activities

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.prof.rssparser.Parser
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.infocenter.*
import com.sapuseven.untis.data.connectivity.UntisApiConstants
import com.sapuseven.untis.data.connectivity.UntisAuthentication
import com.sapuseven.untis.data.connectivity.UntisRequest
import com.sapuseven.untis.data.databases.UserDatabase
import com.sapuseven.untis.helpers.SerializationUtils.getJSON
import com.sapuseven.untis.models.UntisMessage
import com.sapuseven.untis.models.untis.UntisDate
import com.sapuseven.untis.models.untis.params.*
import com.sapuseven.untis.models.untis.response.*
import kotlinx.android.synthetic.main.activity_infocenter.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import org.joda.time.LocalDate
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class InfoCenterActivity : BaseActivity() {
	private val messageList = arrayListOf<UntisMessage>()

	private val messageAdapter = MessageAdapter(this, messageList)

	private var messagesLoading = true

	private var api: UntisRequest = UntisRequest()

	private lateinit var userDatabase: UserDatabase
	private var user: UserDatabase.User? = null

	companion object {
		const val EXTRA_LONG_PROFILE_ID = "com.sapuseven.untis.activities.profileid"

		const val RSS_URL_CHANGE_LATER = "https://www.iwi.hs-karlsruhe.de/intranet/feed/rss/news.xml"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_infocenter)

		val parser = Parser.Builder()
			.context(this)
			.charset(Charset.forName("ISO-8859-7"))
			.cacheExpirationMillis(24L * 60L * 60L * 100L) // one day
			.build()

		GlobalScope.launch {
			try {
				val channel = parser.getChannel(RSS_URL_CHANGE_LATER)
				channel.articles.forEach {
					Log.wtf("AAA", it.title)
					Log.wtf("AAA", it.description)
				}
				// Do something with your data
			} catch (e: Exception) {
				e.printStackTrace()
				// Handle the exception
			}
		}

		/*userDatabase = UserDatabase.createInstance(this)
		user = userDatabase.getUser(intent.getLongExtra(EXTRA_LONG_PROFILE_ID, -1))
		user?.let {
			refreshMessages(it)
		}

		recyclerview_infocenter.layoutManager = LinearLayoutManager(this)

		showList(
			messageAdapter,
			messagesLoading,
			if (messageList.isEmpty()) getString(R.string.infocenter_messages_empty) else ""
		) { user ->
			refreshMessages(user)
		}*/
	}

	private fun showList(
		adapter: RecyclerView.Adapter<*>,
		refreshing: Boolean,
		infoString: String,
		refreshFunction: (user: UserDatabase.User) -> Unit
	) {
		recyclerview_infocenter.adapter = adapter
		swiperefreshlayout_infocenter.isRefreshing = refreshing
		swiperefreshlayout_infocenter.setOnRefreshListener { user?.let { refreshFunction(it) } }
	}

	private fun refreshMessages(user: UserDatabase.User) = GlobalScope.launch(Dispatchers.Main) {
		messagesLoading = true
		loadMessages(user)?.let {
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
	}

	private suspend fun loadMessages(user: UserDatabase.User): List<UntisMessage>? {
		messagesLoading = true

		val query = UntisRequest.UntisRequestQuery(user)

		query.data.method = UntisApiConstants.METHOD_GET_MESSAGES
		query.proxyHost =
			preferences.defaultPrefs.getString("preference_connectivity_proxy_host", null)
		query.data.params = listOf(
			MessageParams(
				UntisDate.fromLocalDate(LocalDate.now()),
				auth = UntisAuthentication.createAuthObject(user)
			)
		)

		val result = api.request(query)
		return result.fold({ data ->
			val untisResponse = getJSON().decodeFromString<MessageResponse>(data)

			untisResponse.result?.messages
		}, { null })
	}
}
