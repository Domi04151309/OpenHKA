package com.sapuseven.untis.activities

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.MensaMenuAdapter
import com.sapuseven.untis.data.mensa.ListItem
import kotlinx.android.synthetic.main.activity_infocenter.*
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

//TODO: add description for additives
//TODO: add mensa selection
class MensaActivity : BaseActivity() {
	private val menu = arrayListOf<ListItem>()

	private val menuAdapter = MensaMenuAdapter(menu)

	private var messagesLoading = true


	companion object {
		const val API_URL: String = "https://www.iwi.hs-karlsruhe.de/iwii/REST"

		const val HARDCODED_ID: String = "2"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_infocenter)

		recyclerview_infocenter.layoutManager = LinearLayoutManager(this)

		refreshMenu()

		showList(
			menuAdapter,
			messagesLoading,
		) { refreshMenu() }
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

	private fun refreshMenu() = GlobalScope.launch(Dispatchers.Main) {
		messagesLoading = true
		loadMenu().let {
			menu.clear()
			menu.addAll(it)
			menuAdapter.notifyDataSetChanged()
		}
		messagesLoading = false
		swiperefreshlayout_infocenter.isRefreshing = false
	}

	//TODO: cache
	private suspend fun loadMenu(): ArrayList<ListItem> {
		val list = arrayListOf<ListItem>()
		val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
		("$API_URL/canteen/v2/$HARDCODED_ID/$date").httpGet()
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
