package com.sapuseven.untis.fragments

import android.content.res.Resources
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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.activities.BaseActivity
import com.sapuseven.untis.adapters.MensaListAdapter
import com.sapuseven.untis.data.GenericParseResult
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
	private val adapter = MensaListAdapter()
	private var dateOffset = 0
	private var idMap: MutableMap<String, Int> = mutableMapOf()
	private var parsedData: GenericParseResult<MensaListItem, MensaPricing> = GenericParseResult()
	private lateinit var stringLoader: StringLoader
	private lateinit var recyclerview: RecyclerView
	private lateinit var swiperefreshlayout: SwipeRefreshLayout
	private lateinit var dropdownMensa: AutoCompleteTextView
	private lateinit var dropdownPricing: AutoCompleteTextView

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
		const val PREFERENCE_MENSA_ID: String = "preference_mensa_id"
		const val DEFAULT_ID: Int = 1
		const val PREFERENCE_MENSA_PRICING_LEVEL: String = "preference_mensa_pricing_level"
		const val DEFAULT_PRICING_LEVEL: String = "Student"

		fun parseAdditives(input: String): Array<CharSequence> {
			val json = JSONArray(input)
			val items = Array<CharSequence>(json.length()) { "" }
			var currentItem: JSONObject
			for (i in 0 until json.length()) {
				currentItem = json.getJSONObject(i)
				items[i] = currentItem.optString("id") + ": " + currentItem.optString("name")
			}
			return items
		}

		fun parseCanteens(
			input: String,
			lambda: (Int, String) -> Unit = { _, _ -> }
		): GenericParseResult<String, Int> {
			val result = GenericParseResult<String, Int>()
			val json = JSONArray(input)
			var currentItem: JSONObject
			var currentTitle: String
			var currentId: Int
			for (i in 0 until json.length()) {
				currentItem = json.getJSONObject(i)
				currentTitle = currentItem.optString("name").replace("Mensa ", "")
				currentId = currentItem.optInt("id")
				result.list.add(currentTitle)
				result.map[currentTitle] = currentId
				lambda(currentId, currentTitle)
			}
			return result
		}

		fun parseMenu(
			resources: Resources,
			input: String,
			pricingLevel: String
		): GenericParseResult<MensaListItem, MensaPricing> {
			val result = GenericParseResult<MensaListItem, MensaPricing>()
			val json = JSONObject(input)
			val mealGroups = json.optJSONArray("mealGroups") ?: JSONArray()
			var meals: JSONArray
			var currentGroup: JSONObject
			var currentMeal: JSONObject
			for (i in 0 until mealGroups.length()) {
				currentGroup = mealGroups.getJSONObject(i)
				result.list.add(MensaListItem("", currentGroup.optString("title"), null))
				meals = currentGroup.optJSONArray("meals") ?: JSONArray()
				for (j in 0 until meals.length()) {
					currentMeal = meals.getJSONObject(j)
					result.map[currentMeal.optString("name")] = MensaPricing(
						currentMeal.optDouble("priceStudent"),
						currentMeal.optDouble("priceGuest"),
						currentMeal.optDouble("priceEmployee"),
						currentMeal.optDouble("pricePupil")
					)
					(currentMeal.optJSONArray("foodAdditiveNumbers") ?: JSONArray()).let {
						result.list.add(
							MensaListItem(
								currentMeal.optString("name"),
								if (it.length() == 0) ""
								else resources.getString(
									R.string.mensa_meal_summary,
									it.join(", ").replace("\"", "")
								),
								null
							).apply {
								price = result.map[title]?.getPriceFromLevel(
									resources,
									pricingLevel
								)
							}
						)
					}
				}
			}
			return result
		}
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
			parsedData.list.forEach { item ->
				if (item.title.isNotEmpty()) {
					item.price =
						parsedData.map[item.title]?.getPriceFromLevel(resources, it.toString())
				}
			}
			adapter.notifyDataSetChanged()
		}

		loadCanteens()

		val tabLayout: TabLayout = root.findViewById(R.id.tab_layout_date)
		val tabOffsetMap: MutableMap<String, Int> = mutableMapOf()
		for (i in 0 until 7) {
			val c = Calendar.getInstance().apply {
				add(Calendar.DATE, i)
			}
			if (
				c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
				|| c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
			) continue

			val date = SimpleDateFormat("E", Locale.getDefault()).format(c.timeInMillis)
			tabOffsetMap[date] = i
			tabLayout.addTab(tabLayout.newTab().apply {
				text = date
			})
		}
		tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab?) {
				dateOffset = tabOffsetMap[tab?.text ?: throw IllegalStateException()]
					?: dateOffset
				refreshParameters(dropdownMensa.text.toString())
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {}
			override fun onTabReselected(tab: TabLayout.Tab?) {}
		})

		recyclerview.layoutManager = LinearLayoutManager(context)
		recyclerview.adapter = adapter
		swiperefreshlayout.setOnRefreshListener { refreshMenu(StringLoader.FLAG_LOAD_SERVER) }

		return root
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.activity_mensa_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return if (item.itemId == R.id.nav_additives) {
			loadAdditives()
			true
		} else false
	}

	private fun loadAdditives() {
		val callback = object : StringDisplay {
			override fun onStringLoaded(string: String) {
				MaterialAlertDialogBuilder(context)
					.setTitle(R.string.mensa_meal_additives)
					.setItems(parseAdditives(string)) { _, _ -> }
					.setPositiveButton(R.string.all_ok) { _, _ -> }
					.show()
			}

			override fun onStringLoadingError(code: Int, loader: StringLoader) {
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
		StringLoader(WeakReference(context), callback, "$API_URL/canteen/v2/foodadditives").load(
			StringLoader.FLAG_LOAD_CACHE
		)
	}

	private fun loadCanteens() {
		val callback = object : StringDisplay {
			override fun onStringLoaded(string: String) {
				val targetId = (activity as BaseActivity).preferences.defaultPrefs
					.getInt(PREFERENCE_MENSA_ID, DEFAULT_ID)
				val canteens = parseCanteens(string) { currentId, currentTitle ->
					if (currentId == targetId) {
						dropdownMensa.setText(currentTitle, false)
						refreshParameters(currentId)
					}
				}
				idMap = canteens.map
				dropdownMensa.setAdapter(
					ArrayAdapter(
						requireContext(),
						android.R.layout.simple_list_item_1,
						canteens.list
					)
				)
				dropdownMensa.addTextChangedListener {
					(activity as BaseActivity).preferences.defaultPrefs.edit().putInt(
						PREFERENCE_MENSA_ID, idMap[it.toString()] ?: DEFAULT_ID
					).apply()
					refreshParameters(it.toString())
				}
			}

			override fun onStringLoadingError(code: Int, loader: StringLoader) {
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
		StringLoader(
			WeakReference(context),
			callback,
			"$API_URL/canteen/names"
		).load(StringLoader.FLAG_LOAD_CACHE)
	}

	private fun refreshMenu(flags: Int) {
		swiperefreshlayout.isRefreshing = true
		stringLoader.load(flags)
	}

	private fun refreshParameters(canteen: Int) {
		val date = SimpleDateFormat("yyyy-MM-dd", Locale.US)
			.format(System.currentTimeMillis() + dateOffset * 86400000)
		stringLoader = StringLoader(
			WeakReference(context),
			this,
			"$API_URL/canteen/v2/$canteen/$date"
		)
		refreshMenu(StringLoader.FLAG_LOAD_CACHE)
	}

	private fun refreshParameters(canteen: String) {
		refreshParameters(idMap[canteen] ?: DEFAULT_ID)
	}

	override fun onStringLoaded(string: String) {
		parsedData = parseMenu(resources, string, dropdownPricing.text.toString())
		adapter.updateItems(parsedData.list)
		swiperefreshlayout.isRefreshing = false
	}

	override fun onStringLoadingError(code: Int, loader: StringLoader) {
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
				adapter.updateItems(arrayListOf())
				swiperefreshlayout.isRefreshing = false
			}
		}
	}
}
