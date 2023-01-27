package com.sapuseven.untis.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.data.lists.PeopleListItem
import com.sapuseven.untis.helpers.drawables.DrawableLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

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

		GlobalScope.launch(Dispatchers.Main) {
			val image = DrawableLoader.load(
				WeakReference(holder.itemView.context),
				items[position].pictureURL,
				DrawableLoader.FLAG_LOAD_CACHE
			)
			if (image != null) {
				holder.ivProfile.setImageDrawable(image)
			}
		}

		holder.tvOverline.text = items[position].overline
		holder.tvSubject.text = items[position].title
		holder.tvBody.text = items[position].summary

		holder.tvOverline.visibility =
			if (items[position].overline.isEmpty()) View.GONE else View.VISIBLE
	}

	class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
		val ivProfile: ImageView = rootView.findViewById(R.id.imageview_itemmessage_profile)
		val tvOverline: TextView = rootView.findViewById(R.id.textview_itemmessage_overline)
		val tvSubject: TextView = rootView.findViewById(R.id.textview_itemmessage_subject)
		val tvBody: TextView = rootView.findViewById(R.id.textview_itemmessage_body)
	}
}
