package com.sapuseven.untis.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.data.lists.GradeListItem
import kotlin.collections.ArrayList

class GradeListAdapter : RecyclerView.Adapter<GradeListAdapter.ViewHolder>() {

	private var items: ArrayList<GradeListItem> = arrayListOf()
	var onClickListener: View.OnClickListener? = null

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
		LayoutInflater.from(parent.context).inflate(R.layout.item_grade, parent, false)
	)

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.itemView.setOnClickListener(onClickListener)
		holder.tvSubject.text = items[position].title
		holder.tvBody.text = items[position].summary
		holder.tvGrade.text = items[position].grade

		holder.tvBody.visibility =
			if (items[position].summary.isEmpty()) View.GONE else View.VISIBLE
		holder.flGrade.visibility =
			if (items[position].grade.isEmpty()) View.GONE else View.VISIBLE
	}

	internal fun updateItems(newItems: ArrayList<GradeListItem>) {
		items = newItems
		notifyDataSetChanged()
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val tvSubject: TextView = rootView.findViewById(R.id.textview_itemmessage_subject)
		val tvBody: TextView = rootView.findViewById(R.id.textview_itemmessage_body)
		val flGrade: FrameLayout = rootView.findViewById(R.id.framelayout_itemmessage_grade)
		val tvGrade: TextView = rootView.findViewById(R.id.textview_itemmessage_grade)
	}
}
