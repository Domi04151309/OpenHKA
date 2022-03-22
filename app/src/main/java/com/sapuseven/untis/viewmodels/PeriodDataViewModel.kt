package com.sapuseven.untis.viewmodels

import androidx.lifecycle.*
import com.github.kittinunf.fuel.core.FuelError
import com.sapuseven.untis.data.connectivity.UntisApiConstants
import com.sapuseven.untis.data.connectivity.UntisAuthentication
import com.sapuseven.untis.data.connectivity.UntisRequest
import com.sapuseven.untis.data.databases.UserDatabase
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.helpers.DateTimeUtils
import com.sapuseven.untis.helpers.SerializationUtils
import com.sapuseven.untis.helpers.timetable.TimetableDatabaseInterface
import com.sapuseven.untis.models.untis.UntisTime
import com.sapuseven.untis.models.untis.params.*
import com.sapuseven.untis.models.untis.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString

class PeriodDataViewModel(
		private val user: UserDatabase.User,
		val item: TimegridItem,
		val timetableDatabaseInterface: TimetableDatabaseInterface?
) : ViewModel() {

	class Factory(private val user: UserDatabase.User?, private val item: TimegridItem?, val timetableDatabaseInterface: TimetableDatabaseInterface?) : ViewModelProvider.Factory {
		override fun <T : ViewModel?> create(modelClass: Class<T>): T {
			return modelClass.getConstructor(UserDatabase.User::class.java, TimegridItem::class.java, TimetableDatabaseInterface::class.java)
					.newInstance(user, item, timetableDatabaseInterface)
		}
	}
}
