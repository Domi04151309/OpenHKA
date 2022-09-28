package com.sapuseven.untis.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.MessageAdapter
import com.sapuseven.untis.helpers.strings.StringLoader
import microsoft.exchange.webservices.data.*
import java.net.URI


class MailFragment : Fragment() {
	private val adapter = MessageAdapter()
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout

	companion object {
		private const val USERNAME = "ads/"
		private const val PASSWORD = ""
		private const val URL = "webmail.h-ka.de"
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

		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = adapter
		swiperefreshlayout.setOnRefreshListener { refreshLocations(StringLoader.FLAG_LOAD_SERVER) }

		refreshLocations(StringLoader.FLAG_LOAD_CACHE)

		adapter.onClickListener = View.OnClickListener {
			//TODO: display email
		}

		return root
	}

	private fun refreshLocations(flags: Int) {
		swiperefreshlayout.isRefreshing = true
		//TODO: load mails
		val service = ExchangeService(ExchangeVersion.Exchange2010_SP2)
		service.credentials =  WebCredentials(USERNAME, PASSWORD)
		service.url = URI(URL)
		pageThroughEntireInbox(service)
		//TODO: display mails
	}

	fun pageThroughEntireInbox(service: ExchangeService) {
		val pageSize = 50
		val view = ItemView(pageSize)
		var findResults: FindItemsResults<Item?>
		do {
			findResults = service.findItems(WellKnownFolderName.Inbox, view)
			for (item in findResults.items) {
				// Do something with the item.
				Log.d("OpenHKA", item?.subject ?: "Undefined")
			}
			view.offset = view.offset + pageSize
		} while (findResults.isMoreAvailable)
	}
}
