package com.sapuseven.untis.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.MainActivity
import com.sapuseven.untis.helpers.drawables.DrawableLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.ref.WeakReference

class PeopleDetailsFragment(private val item: JSONObject) : Fragment() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onPrepareOptionsMenu(menu: Menu) {
		super.onPrepareOptionsMenu(menu)
		menu.clear()
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val root = inflater.inflate(
			R.layout.fragment_people_details_page,
			container,
			false
		)

		GlobalScope.launch(Dispatchers.Main) {
			val image = DrawableLoader.load(
				WeakReference(context),
				item.optString("imageUrl"),
				DrawableLoader.FLAG_LOAD_CACHE
			)
			if (image != null) {
				root.findViewById<ImageView>(R.id.profile).setImageDrawable(image)
			}
		}

		item.optString("academicDegree").let {
			if (it.isEmpty()) {
				root.findViewById<TextView>(R.id.title).visibility = View.GONE
			} else {
				root.findViewById<TextView>(R.id.title).text = item.optString("academicDegree")
			}
		}

		root.findViewById<TextView>(R.id.name).text = item.optString("firstName") + " " +
				item.optString("lastName")

		showInfoOrHide("email", root.findViewById(R.id.tvMail))
		showInfoOrHide("phone", root.findViewById(R.id.tvPhone))
		showInfoOrHide("consultationHour", root.findViewById(R.id.tvConsultationHours))
		showInfoOrHide("remark", root.findViewById(R.id.tvRemark))
		showInfoOrHide("faculty", root.findViewById(R.id.tvFaculty))

		return root
	}

	override fun onStart() {
		super.onStart()
		if (activity is MainActivity) (activity as MainActivity).setFullscreenDialogActionBar(R.string.people_details)
	}

	override fun onStop() {
		super.onStop()
		if (activity is MainActivity) (activity as MainActivity).setDefaultActionBar(R.string.activity_title_people)
	}

	private fun showInfoOrHide(key: String, view: TextView) {
		if (item.isNull(key)) view.visibility = View.GONE
		else view.text = item.optString(key)
	}
}
