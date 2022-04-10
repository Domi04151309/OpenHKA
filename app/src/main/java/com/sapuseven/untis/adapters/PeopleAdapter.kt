package com.sapuseven.untis.adapters

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.data.lists.PeopleListItem

class PeopleAdapter(
	private val items: ArrayList<PeopleListItem>
) : RecyclerView.Adapter<PeopleAdapter.ViewHolder>() {

	var onClickListener: View.OnClickListener? = null

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
		LayoutInflater.from(parent.context).inflate(R.layout.item_person, parent, false)
	)

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.itemView.setOnClickListener(onClickListener)

		holder.tvOverline.text = items[position].overline
		holder.tvSubject.text = items[position].title
		holder.tvBody.text = items[position].summary
		holder.tvBody.movementMethod = LinkMovementMethod.getInstance()

		holder.tvOverline.visibility =
			if (items[position].overline.isEmpty()) View.GONE else View.VISIBLE
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val tvOverline: TextView = rootView.findViewById(R.id.textview_itemmessage_overline)
		val tvSubject: TextView = rootView.findViewById(R.id.textview_itemmessage_subject)
		val tvBody: TextView = rootView.findViewById(R.id.textview_itemmessage_body)
	}
}
