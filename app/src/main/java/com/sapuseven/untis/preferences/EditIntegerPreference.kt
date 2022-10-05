package com.sapuseven.untis.preferences

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import androidx.preference.EditTextPreference
import com.sapuseven.untis.R

class EditIntegerPreference : EditTextPreference {

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, getAttr(context))

    constructor(context: Context) : this(context, null)

    override fun getPersistedString(defaultReturnValue: String?): String {
        val returnValue = defaultReturnValue ?: "0"
        val value: Int = Integer.parseInt(returnValue)
        return getPersistedInt(value).toString()
    }

    override fun persistString(value: String): Boolean {
        val intValue: Int
        try {
            intValue = Integer.parseInt(value)
        } catch (e: Exception) {
            return false
        }
        return persistInt(intValue)
    }

    companion object {
        fun getAttr(context: Context): Int {
            val value = TypedValue()
            context.theme.resolveAttribute(R.attr.editTextPreferenceStyle, value, true)
            return if (value.resourceId != 0) R.attr.editTextPreferenceStyle
            else android.R.attr.editTextPreferenceStyle
        }
    }
}
