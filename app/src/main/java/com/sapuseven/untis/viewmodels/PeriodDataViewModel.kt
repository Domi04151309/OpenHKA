package com.sapuseven.untis.viewmodels

import androidx.lifecycle.*
import com.sapuseven.untis.data.databases.UserDatabase
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.helpers.timetable.TimetableDatabaseInterface

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
