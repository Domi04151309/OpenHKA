package com.sapuseven.untis.adapters

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.data.lists.ListItem

class MessageAdapter(
	private val items: ArrayList<ListItem>
) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

	var onClickListener: View.OnClickListener? = null

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
		LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
	)

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.itemView.setOnClickListener(onClickListener)

		holder.tvSubject.text = items[position].title
		holder.tvBody.text = items[position].summary
		holder.tvBody.movementMethod = LinkMovementMethod.getInstance()

		holder.tvBody.visibility = if (items[position].summary.isEmpty()) View.GONE else View.VISIBLE
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val tvSubject: TextView = rootView.findViewById(R.id.textview_itemmessage_subject)
		val tvBody: TextView = rootView.findViewById(R.id.textview_itemmessage_body)
	}
}
