package com.sapuseven.untis.adapters

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.data.lists.JobItem

class JobAdapter(
	private var items: ArrayList<JobItem> = arrayListOf()
) : RecyclerView.Adapter<JobAdapter.ViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
		LayoutInflater.from(parent.context).inflate(R.layout.item_rss, parent, false)
	)

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.tvTime.text = items[position].company
		holder.tvSubject.text = items[position].title
		holder.tvBody.text =
			HtmlCompat.fromHtml(items[position].summary, HtmlCompat.FROM_HTML_MODE_COMPACT)
		holder.tvBody.movementMethod = LinkMovementMethod.getInstance()
	}

	internal fun updateItems(newItems: ArrayList<JobItem>) {
		items = newItems
		notifyDataSetChanged()
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val tvTime: TextView = rootView.findViewById(R.id.textview_rss_time)
		val tvSubject: TextView = rootView.findViewById(R.id.textview_rss_subject)
		val tvBody: TextView = rootView.findViewById(R.id.textview_rss_body)
	}
}
