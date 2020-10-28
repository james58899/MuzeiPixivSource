package one.oktw.muzeipixivsource.activity.preference

import android.content.Context
import android.content.res.TypedArray
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.NumberPicker
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import one.oktw.muzeipixivsource.R

class NumberPickerPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {
    var value = 0
        set(value) {
            field = value
            persistInt(value)
        }
    val min: Int
    val max: Int

    init {
        dialogLayoutResource = R.layout.numberpicker_dialog
        dialogIcon = null

        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.NumberPicker, 0, 0)
        try {
            min = a.getInt(R.styleable.NumberPicker_min, 0)
            max = a.getInt(R.styleable.NumberPicker_max, Int.MAX_VALUE - 1)
        } finally {
            a.recycle()
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, 1)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedInt(defaultValue as? Int ?: value)
    }

    override fun onSaveInstanceState(): Parcelable {
        return if (isPersistent) {
            super.onSaveInstanceState()
        } else {
            SavedState(super.onSaveInstanceState()).also { it.value = value }
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

    class Fragment : PreferenceDialogFragmentCompat() {
        companion object {
            fun newInstance(key: String): Fragment {
                return Fragment().apply { arguments = Bundle(1).apply { putString(ARG_KEY, key) } }
            }
        }

        private val saveKey = "NumberPickerPreferenceFragment.value"
        private var value = 0
        private lateinit var picker: NumberPicker

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            value = savedInstanceState?.getInt(saveKey) ?: (preference as NumberPickerPreference).value
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)

            picker.clearFocus()
            value = picker.value
            outState.putInt(saveKey, value)
        }

        override fun onBindDialogView(view: View) {
            super.onBindDialogView(view)

            val preference = preference as NumberPickerPreference
            picker = view.findViewById(R.id.numberPicker)

            picker.minValue = preference.min
            picker.maxValue = preference.max
            picker.value = value
        }

        override fun onDialogClosed(positiveResult: Boolean) {
            if (positiveResult) {
                picker.clearFocus()
                value = picker.value
                (preference as NumberPickerPreference).value = value
            }
        }
    }
}
