package one.oktw.muzeipixivsource.activity.preference

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.preference.DialogPreference
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.NumberPicker
import one.oktw.muzeipixivsource.R

class NumberPickerPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {
    private var value = 0
    private val min: Int
    private val max: Int
    private lateinit var picker: NumberPicker

    init {
        dialogLayoutResource = R.layout.numberpicker_dialog
        dialogIcon = null

        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.NumberPicker, 0, 0)
        try {
            min = a.getInt(R.styleable.NumberPicker_min, 0)
            max = a.getInt(R.styleable.NumberPicker_max, 100)
        } finally {
            a.recycle()
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, 1)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (restorePersistedValue) {
            value = getPersistedInt(1)
        } else {
            value = defaultValue as Int
            persistInt(value)
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        picker = view.findViewById(R.id.numberPicker)

        picker.minValue = min
        picker.maxValue = max
        picker.value = value
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            picker.clearFocus()
            value = picker.value
            persistInt(value)
        } else {
            value = getPersistedInt(1)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return if (this::picker.isInitialized) {
            SavedState(super.onSaveInstanceState()).apply { value = picker.value }
        } else {
            super.onSaveInstanceState()
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        value = state.value

        super.onRestoreInstanceState(state.superState)
    }

    private class SavedState(superState: Parcelable) : Preference.BaseSavedState(superState) {
        var value = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(value)
        }
    }
}
