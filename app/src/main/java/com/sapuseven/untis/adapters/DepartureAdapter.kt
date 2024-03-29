package com.sapuseven.untis.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.data.lists.DepartureListItem

class DepartureAdapter : RecyclerView.Adapter<DepartureAdapter.ViewHolder>() {

	private var items: ArrayList<DepartureListItem> = arrayListOf()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
		LayoutInflater.from(parent.context).inflate(R.layout.item_departure, parent, false)
	)

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.tvLine.text = items[position].line
		holder.tvSubject.text = items[position].title
		holder.tvBody.text = items[position].summary
		holder.ivLow.visibility = if (items[position].lowFloor) View.VISIBLE else View.GONE
	}

	internal fun updateItems(newItems: ArrayList<DepartureListItem>) {
		items = newItems
		notifyDataSetChanged()
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val tvLine: TextView = rootView.findViewById(R.id.textview_itemmessage_line)
		val tvSubject: TextView = rootView.findViewById(R.id.textview_itemmessage_subject)
		val tvBody: TextView = rootView.findViewById(R.id.textview_itemmessage_body)
		val ivLow: ImageView = rootView.findViewById(R.id.imageview_itemmessage_low)
	}
}
