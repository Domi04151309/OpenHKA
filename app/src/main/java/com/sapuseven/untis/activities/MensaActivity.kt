package com.sapuseven.untis.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.MensaMenuAdapter
import com.sapuseven.untis.data.lists.ListItem
import kotlinx.android.synthetic.main.activity_infocenter.recyclerview_infocenter
import kotlinx.android.synthetic.main.activity_infocenter.swiperefreshlayout_infocenter
import kotlinx.android.synthetic.main.activity_mensa.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MensaActivity : BaseActivity() {
	private val menu = arrayListOf<ListItem>()
	private val menuAdapter = MensaMenuAdapter(menu)
	private var menuLoading = true
	private val idMap: MutableMap<String, Int> = mutableMapOf()
	private var currentID = DEFAULT_ID

	companion object {
		private const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"
		private const val DEFAULT_ID: Int = 1
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_mensa)

		recyclerview_infocenter.layoutManager = LinearLayoutManager(this)

		loadCanteens()

		(dropdown.editText as AutoCompleteTextView).addTextChangedListener {
			currentID = idMap[it.toString()] ?: DEFAULT_ID
			refreshMenu()
		}

		showList(
			menuAdapter,
			menuLoading,
		) { refreshMenu() }
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_mensa_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return if (item.title == resources.getString(R.string.mensa_meal_additives)) {
			loadAdditives()
			true
		} else false
	}

	private fun loadAdditives() = GlobalScope.launch(Dispatchers.Main) {
		"$API_URL/canteen/v2/foodadditives".httpGet()
			.awaitStringResult()
			.fold({ data ->
				val json = JSONArray(data)
				val items = Array<CharSequence>(json.length()) { "" }
				var currentItem: JSONObject
				for (i in 0 until json.length()) {
					currentItem = json.getJSONObject(i)
					items[i] = currentItem.optString("id") + ": " + currentItem.optString("name")
				}
				MaterialAlertDialogBuilder(this@MensaActivity)
					.setTitle(R.string.mensa_meal_additives)
					.setItems(items) { _, _ -> }
					.setPositiveButton(R.string.all_ok) { _, _ -> }
					.show()
			}, {
				//TODO: handle error
			})
	}

	private fun showList(
		adapter: RecyclerView.Adapter<*>,
		refreshing: Boolean,
		refreshFunction: () -> Unit
	) {
		recyclerview_infocenter.adapter = adapter
		swiperefreshlayout_infocenter.isRefreshing = refreshing
		swiperefreshlayout_infocenter.setOnRefreshListener { refreshFunction() }
	}

	private fun loadCanteens() = GlobalScope.launch(Dispatchers.Main) {
		"$API_URL/canteen/names".httpGet()
			.awaitStringResult()
			.fold({ data ->
				val json = JSONArray(data)
				val canteens = MutableList(json.length()) { "" }
				var currentItem: JSONObject
				var currentTitle: String
				for (i in 0 until json.length()) {
					currentItem = json.getJSONObject(i)
					currentTitle = currentItem.optString("name").replace("Mensa ", "")
					idMap[currentTitle] = currentItem.optInt("id")
					canteens[i] = currentTitle
				}
				(dropdown.editText as AutoCompleteTextView).let {
					it.setAdapter(
						ArrayAdapter(
							this@MensaActivity,
							android.R.layout.simple_list_item_1,
							canteens
						)
					)
					it.setText(canteens[0], false)
				}
			}, {
				//TODO: handle error
			})
	}

	private fun refreshMenu() = GlobalScope.launch(Dispatchers.Main) {
		menuLoading = true
		loadMenu().let {
			menu.clear()
			menu.addAll(it)
			menuAdapter.notifyDataSetChanged()
		}
		menuLoading = false
		swiperefreshlayout_infocenter.isRefreshing = false
	}

	//TODO: cache
	private suspend fun loadMenu(): ArrayList<ListItem> {
		val list = arrayListOf<ListItem>()
		val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
		"$API_URL/canteen/v2/$currentID/$date".httpGet()
			.awaitStringResult()
			.fold({ data ->
				val json = JSONObject(data)
				val mealGroups = json.optJSONArray("mealGroups") ?: JSONArray()
				var meals: JSONArray
				var currentGroup: JSONObject
				var currentMeal: JSONObject
				for (i in 0 until mealGroups.length()) {
					currentGroup = mealGroups.getJSONObject(i)
					list.add(ListItem("", currentGroup.optString("title")))
					meals = currentGroup.optJSONArray("meals") ?: JSONArray()
					for (j in 0 until meals.length()) {
						currentMeal = meals.getJSONObject(j)
						list.add(
							ListItem(
								currentMeal.optString("name"),
								generateSummary(currentMeal)
							)
						)
					}
				}
			}, {
				//TODO: handle error
			})
		return list
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
