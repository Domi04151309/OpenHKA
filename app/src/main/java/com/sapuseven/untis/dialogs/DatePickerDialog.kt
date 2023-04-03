package com.sapuseven.untis.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sapuseven.untis.R
import org.joda.time.DateTime

class DatePickerDialog : DialogFragment() {
	var dateSetListener: android.app.DatePickerDialog.OnDateSetListener? = null

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val datePicker = DatePicker(requireContext())
		arguments?.let {
			datePicker.updateDate(
				it.getInt("year"),
				it.getInt("month") - 1,
				it.getInt("day")
			)
		} ?: run {
			val now = DateTime.now()
			datePicker.updateDate(
				now.year,
				now.monthOfYear - 1,
				now.dayOfMonth
			)
		}

		val dialog = MaterialAlertDialogBuilder(requireContext())
			.setView(datePicker)
			.create()

		dialog.setButton(
			android.app.DatePickerDialog.BUTTON_POSITIVE,
			getString(R.string.all_ok)
		) { _, _ ->
			dateSetListener?.onDateSet(
				datePicker,
				datePicker.year,
				datePicker.month,
				datePicker.dayOfMonth
			)
		}

		dialog.setButton(
			android.app.DatePickerDialog.BUTTON_NEGATIVE,
			getString(R.string.all_cancel)
		) { _, _ -> }

		dialog.setButton(
			android.app.DatePickerDialog.BUTTON_NEUTRAL,
			getString(R.string.all_dialog_datepicker_button_today)
		) { _, _ ->
			val dateTime = DateTime.now()
			dateSetListener?.onDateSet(
				datePicker,
				dateTime.year,
				dateTime.monthOfYear - 1,
				dateTime.dayOfMonth
			)
		}
		return dialog
	}
}
