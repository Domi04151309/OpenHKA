package com.sapuseven.untis.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.data.lists.StationItem

class StationAdapter(
	private val items: ArrayList<StationItem> = arrayListOf()
) : RecyclerView.Adapter<StationAdapter.ViewHolder>() {

	var onClickListener: View.OnClickListener? = null

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
		LayoutInflater.from(parent.context).inflate(R.layout.item_station, parent, false)
	)

	override fun getItemCount(): Int = items.size

	@SuppressLint("ClickableViewAccessibility")
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.itemView.setOnClickListener(onClickListener)
		holder.rvDepartures.setOnTouchListener { _, event ->
			holder.itemView.onTouchEvent(event)
			false
		}

		holder.tvSubject.text = items[position].title

		holder.rvDepartures.isNestedScrollingEnabled = false
		holder.rvDepartures.layoutManager = LinearLayoutManager(holder.itemView.context)
		holder.rvDepartures.adapter = DepartureSmallAdapter(items[position].departures)
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val tvSubject: TextView = rootView.findViewById(R.id.textview_itemmessage_subject)
		val rvDepartures: RecyclerView = rootView.findViewById(R.id.recyclerview_itemmessage_departures)
	}
}
