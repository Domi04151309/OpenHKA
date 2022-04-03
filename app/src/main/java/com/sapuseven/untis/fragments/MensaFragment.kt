package com.sapuseven.untis.fragments

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.MessageAdapter
import com.sapuseven.untis.data.lists.ListItem
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class MensaFragment : Fragment(), StringDisplay {
	private val menu = arrayListOf<ListItem>()
	private val menuAdapter = MessageAdapter(menu)
	private var menuLoading = true
	private val idMap: MutableMap<String, Int> = mutableMapOf()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout
	private lateinit var dropdown: AutoCompleteTextView

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
		private const val DEFAULT_ID: Int = 1
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(
			R.layout.fragment_mensa,
			container,
			false
		)

		recyclerview = root.findViewById(R.id.recyclerview_infocenter)
		swiperefreshlayout = root.findViewById(R.id.swiperefreshlayout_infocenter)
		dropdown =
			((root.findViewById(R.id.dropdown) as TextInputLayout).editText as AutoCompleteTextView)

		loadCanteens()

		dropdown.addTextChangedListener {
			val currentID = idMap[it.toString()] ?: DEFAULT_ID
			val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
			stringLoader = StringLoader(
				WeakReference(context),
				this,
				"$API_URL/canteen/v2/$currentID/$date"
			)
			refreshMenu(StringLoader.FLAG_LOAD_CACHE)
		}

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = menuAdapter
		swiperefreshlayout.isRefreshing = menuLoading
		swiperefreshlayout.setOnRefreshListener { refreshMenu(StringLoader.FLAG_LOAD_SERVER) }

		return root
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.activity_mensa_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return if (item.title == resources.getString(R.string.mensa_meal_additives)) {
			loadAdditives()
			true
		} else false
	}

	private fun loadAdditives() {
		lateinit var loader: StringLoader
		val callback = object : StringDisplay {
			override fun onStringLoaded(string: String) {
				val json = JSONArray(string)
				val items = Array<CharSequence>(json.length()) { "" }
				var currentItem: JSONObject
				for (i in 0 until json.length()) {
					currentItem = json.getJSONObject(i)
					items[i] = currentItem.optString("id") + ": " + currentItem.optString("name")
				}
				MaterialAlertDialogBuilder(context)
					.setTitle(R.string.mensa_meal_additives)
					.setItems(items) { _, _ -> }
					.setPositiveButton(R.string.all_ok) { _, _ -> }
					.show()
			}

			override fun onStringLoadingError(code: Int) {
				when (code) {
					StringLoader.CODE_CACHE_MISSING -> loader.repeat(
						StringLoader.FLAG_LOAD_SERVER
					)
					else -> {
						MaterialAlertDialogBuilder(context)
							.setTitle(R.string.mensa_meal_additives)
							.setMessage(R.string.errors_failed_loading_from_server_message)
							.setPositiveButton(R.string.all_ok) { _, _ -> }
							.show()
					}
				}
			}
		}
		loader = StringLoader(WeakReference(context), callback, "$API_URL/canteen/v2/foodadditives")
		loader.load(StringLoader.FLAG_LOAD_CACHE)
	}

	private fun loadCanteens() {
		lateinit var loader: StringLoader
		val callback = object : StringDisplay {
			override fun onStringLoaded(string: String) {
				val json = JSONArray(string)
				val canteens = MutableList(json.length()) { "" }
				var currentItem: JSONObject
				var currentTitle: String
				for (i in 0 until json.length()) {
					currentItem = json.getJSONObject(i)
					currentTitle = currentItem.optString("name").replace("Mensa ", "")
					idMap[currentTitle] = currentItem.optInt("id")
					canteens[i] = currentTitle
				}
				dropdown.let {
					it.setAdapter(
						ArrayAdapter(
							requireContext(),
							android.R.layout.simple_list_item_1,
							canteens
						)
					)
					it.setText(canteens[0], false)
				}
			}

			override fun onStringLoadingError(code: Int) {
				when (code) {
					StringLoader.CODE_CACHE_MISSING -> loader.repeat(
						StringLoader.FLAG_LOAD_SERVER
					)
					else -> {
						MaterialAlertDialogBuilder(context)
							.setTitle(R.string.activity_title_mensa)
							.setMessage(R.string.errors_failed_loading_from_server_message)
							.setPositiveButton(R.string.all_ok) { _, _ -> }
							.show()
					}
				}
			}
		}
		loader = StringLoader(WeakReference(context), callback, "$API_URL/canteen/names")
		loader.load(StringLoader.FLAG_LOAD_CACHE)
	}

	private fun refreshMenu(flags: Int) {
		menuLoading = true
		stringLoader.load(flags)
	}

	override fun onStringLoaded(string: String) {
		menu.clear()
		val json = JSONObject(string)
		val mealGroups = json.optJSONArray("mealGroups") ?: JSONArray()
		var meals: JSONArray
		var currentGroup: JSONObject
		var currentMeal: JSONObject
		for (i in 0 until mealGroups.length()) {
			currentGroup = mealGroups.getJSONObject(i)
			menu.add(ListItem("", currentGroup.optString("title")))
			meals = currentGroup.optJSONArray("meals") ?: JSONArray()
			for (j in 0 until meals.length()) {
				currentMeal = meals.getJSONObject(j)
				menu.add(
					ListItem(
						currentMeal.optString("name"),
						generateSummary(currentMeal)
					)
				)
			}
		}
		menuAdapter.notifyDataSetChanged()
		menuLoading = false
		swiperefreshlayout.isRefreshing = false
	}

	override fun onStringLoadingError(code: Int) {
		when (code) {
			StringLoader.CODE_CACHE_MISSING -> stringLoader.repeat(
				StringLoader.FLAG_LOAD_SERVER
			)
			else -> {
				Toast.makeText(
					context,
					R.string.errors_failed_loading_from_server_message,
					Toast.LENGTH_LONG
				).show()
				menu.clear()
				menuAdapter.notifyDataSetChanged()
				menuLoading = false
				swiperefreshlayout.isRefreshing = false
			}
		}
	}

	private fun generateSummary(meal: JSONObject): String {
		val df = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.getDefault()))
		return resources.getString(
			R.string.mensa_meal_summary,
			(meal.optJSONArray("foodAdditiveNumbers") ?: JSONArray()).join(", ").replace("\"", ""),
			df.format(meal.optDouble("priceStudent")),
			df.format(meal.optDouble("priceGuest")),
			df.format(meal.optDouble("priceEmployee")),
			df.format(meal.optDouble("pricePupil"))
		)
	}
}
