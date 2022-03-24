package com.sapuseven.untis.viewmodels

import androidx.lifecycle.*
import com.sapuseven.untis.data.timetable.TimegridItem

class PeriodDataViewModel(
		val item: TimegridItem,
) : ViewModel() {

	class Factory(private val item: TimegridItem?) : ViewModelProvider.Factory {
		override fun <T : ViewModel?> create(modelClass: Class<T>): T {
			return modelClass.getConstructor(TimegridItem::class.java)
					.newInstance(item)
		}
	}
}
