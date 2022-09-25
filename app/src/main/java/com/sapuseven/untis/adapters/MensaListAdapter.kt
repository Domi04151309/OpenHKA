package com.sapuseven.untis.adapters

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.data.lists.MensaListItem
import com.sapuseven.untis.data.lists.StudyPlaceListItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.ArrayList

class MensaListAdapter() : RecyclerView.Adapter<MensaListAdapter.ViewHolder>() {

	private var items: ArrayList<MensaListItem> = arrayListOf()
	private val formatter = DecimalFormat(
		"0.00", DecimalFormatSymbols.getInstance(Locale.getDefault())
	)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
		LayoutInflater.from(parent.context).inflate(R.layout.item_mensa, parent, false)
	)

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.tvSubject.text = items[position].title
		holder.tvBody.text = items[position].summary
		holder.tvBody.movementMethod = LinkMovementMethod.getInstance()

		holder.tvPrice.text =
			if (items[position].price == null) ""
			else holder.itemView.context.resources.getString(
				R.string.mensa_meal_price, formatter.format(items[position].price)
			)

		holder.tvBody.visibility =
			if (items[position].summary.isEmpty()) View.GONE else View.VISIBLE
	}

	internal fun updateItems(newItems: ArrayList<MensaListItem>) {
		items = newItems
		notifyDataSetChanged()
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val tvSubject: TextView = rootView.findViewById(R.id.textview_itemmessage_subject)
		val tvBody: TextView = rootView.findViewById(R.id.textview_itemmessage_body)
		val tvPrice: TextView = rootView.findViewById(R.id.textview_itemmessage_price)
	}
}
