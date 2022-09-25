package com.sapuseven.untis.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.data.lists.ChecklistItem

class ChecklistAdapter(
	private var items: ArrayList<ChecklistItem> = arrayListOf()
) : RecyclerView.Adapter<ChecklistAdapter.ViewHolder>() {

	var onClickListener: View.OnClickListener? = null

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
		LayoutInflater.from(parent.context).inflate(R.layout.item_checklist, parent, false)
	)

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.itemView.setOnClickListener(onClickListener)

		holder.tvSubject.text = items[position].title
		holder.tvBody.text = items[position].summary
		holder.ivCheck.visibility = if (items[position].checked) View.VISIBLE else View.GONE
	}

	internal fun updateItems(newItems: ArrayList<ChecklistItem>) {
		items = newItems
		notifyDataSetChanged()
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val tvSubject: TextView = rootView.findViewById(R.id.textview_itemmessage_subject)
		val tvBody: TextView = rootView.findViewById(R.id.textview_itemmessage_body)
		val ivCheck: ImageView = rootView.findViewById(R.id.textview_itemmessage_check)
	}
}
