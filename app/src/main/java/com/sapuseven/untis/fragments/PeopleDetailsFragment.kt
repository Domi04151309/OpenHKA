package com.sapuseven.untis.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.MainActivity
import com.sapuseven.untis.helpers.drawables.DrawableLoader
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

class PeopleDetailsFragment(private val item: JSONObject) : Fragment() {

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/hskampus-broker/api"
	}

	private var coordinates: String = ""

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		menu.clear()
		if (!item.isNull("room")) inflater.inflate(R.menu.fragment_people_details_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return if (item.itemId == R.id.maps) {
			if (coordinates.isNotEmpty()) {
				val mapIntent = Intent(
					Intent.ACTION_VIEW, Uri.parse(
						"geo:0,0?q=$coordinates"
					)
				)
				mapIntent.setPackage("com.google.android.apps.maps")
				startActivity(mapIntent)
			} else {
				MaterialAlertDialogBuilder(context)
					.setTitle(R.string.all_details)
					.setMessage(R.string.errors_failed_loading_from_server_message)
					.setPositiveButton(R.string.all_ok) { _, _ -> }
					.show()
			}
			true
		} else false
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

		if (!item.isNull("room")) {
			lateinit var loader: StringLoader
			val callback = object : StringDisplay {
				override fun onStringLoaded(string: String) {
					val json = JSONArray(string)
					var currentItem: JSONObject
					for (i in 0 until json.length()) {
						currentItem = json.getJSONObject(i)
						if (
							currentItem.optInt("id") == (item.optJSONObject("room")
								?: JSONObject()).optInt("id")
						) {
							lateinit var innerLoader: StringLoader
							val innerCallback = object : StringDisplay {
								override fun onStringLoaded(string: String) {
									val innerJson = JSONArray(string)
									var innerCurrentItem: JSONObject
									for (j in 0 until innerJson.length()) {
										innerCurrentItem = innerJson.getJSONObject(j)
										if (
											innerCurrentItem.optInt("id") == (currentItem.optJSONObject(
												"building"
											) ?: JSONObject()).optInt("id")
										) {
											root.findViewById<TextView>(R.id.tvRoom).apply {
												visibility = View.VISIBLE
												text = innerCurrentItem.optString("name") +
														" " + currentItem.optString("name")
											}
											coordinates = innerCurrentItem
												.optString("coordinatesCenter")
											return
										}
									}
								}

								override fun onStringLoadingError(code: Int) =
									defaultOnError(innerLoader, code)
							}
							innerLoader = StringLoader(
								WeakReference(context),
								innerCallback,
								"${API_URL}/buildings"
							)
							innerLoader.load(StringLoader.FLAG_LOAD_CACHE)
							return
						}
					}
				}

				override fun onStringLoadingError(code: Int) = defaultOnError(loader, code)
			}
			loader = StringLoader(WeakReference(context), callback, "${API_URL}/rooms")
			loader.load(StringLoader.FLAG_LOAD_CACHE)
		}

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

	internal fun defaultOnError(loader: StringLoader, code: Int) {
		when (code) {
			StringLoader.CODE_CACHE_MISSING -> loader.repeat(
				StringLoader.FLAG_LOAD_SERVER
			)
			else -> {
				Toast.makeText(
					context,
					R.string.errors_failed_loading_from_server_message,
					Toast.LENGTH_LONG
				).show()
			}
		}
	}
}
