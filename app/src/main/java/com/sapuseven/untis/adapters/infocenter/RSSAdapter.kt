package com.sapuseven.untis.adapters.infocenter

import android.content.Context
import android.text.format.DateFormat
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.prof.rssparser.Article
import com.sapuseven.untis.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RSSAdapter(
	private val messageList: List<Article> = ArrayList()
) : RecyclerView.Adapter<RSSAdapter.ViewHolder>() {

	companion object {
		private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

		fun parseDate(context: Context, date: String): String {
			val millis = try {
				(dateFormat.parse(date) ?: Date()).time
			} catch (e: Exception) {
				date.toLong() * 1000
			}
			return DateFormat.getMediumDateFormat(context).format(millis) +
					", " +
					DateFormat.getTimeFormat(context).format(millis)
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val v = LayoutInflater.from(parent.context).inflate(R.layout.item_rss, parent, false)
		return ViewHolder(v)
	}

	override fun getItemCount(): Int = messageList.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val message = messageList[position]

		holder.tvTime.text = parseDate(holder.itemView.context, message.pubDate ?: "")
		holder.tvSubject.text = message.title
		holder.tvBody.text =
			HtmlCompat.fromHtml(message.description ?: "", HtmlCompat.FROM_HTML_MODE_COMPACT)
		holder.tvBody.movementMethod = LinkMovementMethod.getInstance()
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val tvTime: TextView = rootView.findViewById(R.id.textview_rss_time)
		val tvSubject: TextView = rootView.findViewById(R.id.textview_rss_subject)
		val tvBody: TextView = rootView.findViewById(R.id.textview_rss_body)
	}
}
