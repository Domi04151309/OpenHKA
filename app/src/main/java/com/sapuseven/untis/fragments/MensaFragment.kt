package com.sapuseven.untis.fragments

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.BaseActivity
import com.sapuseven.untis.adapters.MensaListAdapter
import com.sapuseven.untis.data.lists.MensaListItem
import com.sapuseven.untis.data.lists.MensaPricing
import com.sapuseven.untis.helpers.strings.StringLoader
import com.sapuseven.untis.interfaces.StringDisplay
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class MensaFragment : Fragment(), StringDisplay {
	private val menu = arrayListOf<MensaListItem>()
	private val menuAdapter = MensaListAdapter(menu)
	private var menuLoading = true
	private var dateOffset = 0
	private val idMap: MutableMap<String, Int> = mutableMapOf()
	private val pricingMap: MutableMap<String, MensaPricing> = mutableMapOf()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout
	private lateinit var textViewDate: TextView
	private lateinit var dropdownMensa: AutoCompleteTextView
	private lateinit var dropdownPricing: AutoCompleteTextView

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
		private const val PREFERENCE_MENSA_PRICING_LEVEL: String = "preference_mensa_pricing_level"
		private const val DEFAULT_ID: Int = 1
		private const val DEFAULT_PRICING_LEVEL: String = "Student"
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
		dropdownMensa =
			((root.findViewById(R.id.dropdown_mensa) as TextInputLayout).editText as AutoCompleteTextView)
		dropdownPricing =
			((root.findViewById(R.id.dropdown_pricing) as TextInputLayout).editText as AutoCompleteTextView)

		dropdownPricing.setAdapter(
			ArrayAdapter(
				requireContext(),
				android.R.layout.simple_list_item_1,
				resources.getStringArray(R.array.mensa_pricing_values)
			)
		)
		dropdownPricing.setText(
			(activity as BaseActivity).preferences.defaultPrefs
				.getString(PREFERENCE_MENSA_PRICING_LEVEL, DEFAULT_PRICING_LEVEL),
			false
		)
		dropdownPricing.addTextChangedListener {
			(activity as BaseActivity).preferences.defaultPrefs.edit().putString(
				PREFERENCE_MENSA_PRICING_LEVEL, it.toString()
			).apply()
			menu.forEach { item ->
				if (item.title.isNotEmpty()) {
					item.price =
						pricingMap[item.title]?.getPriceFromLevel(requireContext(), it.toString())
				}
			}
			menuAdapter.notifyDataSetChanged()
		}

		loadCanteens()

		dropdownMensa.addTextChangedListener {
			refreshParameters(it.toString())
		}

		textViewDate = root.findViewById(R.id.textview_date)
		textViewDate.setOnClickListener {
			if (dateOffset == 0) {
				dateOffset++
				textViewDate.text = resources.getString(R.string.all_tomorrow)
			} else if (dateOffset == 1) {
				dateOffset--
				textViewDate.text = resources.getString(R.string.all_today)
			}
			refreshParameters(dropdownMensa.text.toString())
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
				dropdownMensa.setAdapter(
					ArrayAdapter(
						requireContext(),
						android.R.layout.simple_list_item_1,
						canteens
					)
				)
				dropdownMensa.setText(canteens[0], false)
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

	private fun refreshParameters(canteen: String) {
		val currentID = idMap[canteen] ?: DEFAULT_ID
		val date = SimpleDateFormat("yyyy-MM-dd", Locale.US)
			.format(System.currentTimeMillis() + dateOffset * 86400000)
		stringLoader = StringLoader(
			WeakReference(context),
			this,
			"$API_URL/canteen/v2/$currentID/$date"
		)
		refreshMenu(StringLoader.FLAG_LOAD_CACHE)
	}

	override fun onStringLoaded(string: String) {
		menu.clear()
		pricingMap.clear()
		val json = JSONObject(string)
		val mealGroups = json.optJSONArray("mealGroups") ?: JSONArray()
		var meals: JSONArray
		var currentGroup: JSONObject
		var currentMeal: JSONObject
		for (i in 0 until mealGroups.length()) {
			currentGroup = mealGroups.getJSONObject(i)
			menu.add(MensaListItem("", currentGroup.optString("title"), null))
			meals = currentGroup.optJSONArray("meals") ?: JSONArray()
			for (j in 0 until meals.length()) {
				currentMeal = meals.getJSONObject(j)
				pricingMap[currentMeal.optString("name")] = MensaPricing(
					currentMeal.optDouble("priceStudent"),
					currentMeal.optDouble("priceGuest"),
					currentMeal.optDouble("priceEmployee"),
					currentMeal.optDouble("pricePupil")
				)
				(currentMeal.optJSONArray("foodAdditiveNumbers") ?: JSONArray()).let {
					menu.add(
						MensaListItem(
							currentMeal.optString("name"),
							if (it.length() == 0) ""
							else resources.getString(
								R.string.mensa_meal_summary,
								it.join(", ").replace("\"", "")
							),
							null
						).apply {
							price = pricingMap[title]?.getPriceFromLevel(
								requireContext(),
								dropdownPricing.text.toString()
							)
						}
					)
				}
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
}
