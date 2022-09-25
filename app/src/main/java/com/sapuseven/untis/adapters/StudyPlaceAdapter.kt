package com.sapuseven.untis.adapters

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.data.lists.StudyPlaceListItem

class StudyPlaceAdapter() : RecyclerView.Adapter<StudyPlaceAdapter.ViewHolder>() {

	var onClickListener: View.OnClickListener? = null
	private var items: ArrayList<StudyPlaceListItem> = arrayListOf()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
		LayoutInflater.from(parent.context).inflate(R.layout.item_study_place, parent, false)
	)

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.itemView.setOnClickListener(onClickListener)

		holder.progressBar.progress = items[position].value
		holder.progressBar.max = items[position].max
		holder.tvOverline.text = items[position].overline
		holder.tvSubject.text = items[position].title
		holder.tvBody.text = items[position].summary
		holder.tvBody.movementMethod = LinkMovementMethod.getInstance()
	}

	internal fun updateItems(newItems: ArrayList<StudyPlaceListItem>) {
		items = newItems
		notifyDataSetChanged()
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val progressBar: ProgressBar = rootView.findViewById(R.id.progressbar_itemmessage)
		val tvOverline: TextView = rootView.findViewById(R.id.textview_itemmessage_overline)
		val tvSubject: TextView = rootView.findViewById(R.id.textview_itemmessage_subject)
		val tvBody: TextView = rootView.findViewById(R.id.textview_itemmessage_body)
	}
}
