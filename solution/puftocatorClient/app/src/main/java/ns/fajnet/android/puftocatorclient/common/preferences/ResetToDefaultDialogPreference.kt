package ns.fajnet.android.puftocatorclient.common.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.AttributeSet
import androidx.fragment.app.setFragmentResult
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceManager
import ns.fajnet.android.puftocatorclient.R

class ResetToDefaultDialogPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs),
    SharedPreferences.OnSharedPreferenceChangeListener {

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        onPreferenceChangeListener?.onPreferenceChange(this, true)
    }
}

// #####################################################################################################################

class ResetToDefaultDialogPreferenceDialog : PreferenceDialogFragmentCompat() {

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit()
            preferencesEditor.clear()
            PreferenceManager.setDefaultValues(requireContext(), R.xml.root_preferences, true)
            preferencesEditor.apply()

            val result = Bundle()
            result.putBoolean("key", true)
            setFragmentResult("requestKey", result)
        }
    }

    // companion object ------------------------------------------------------------------------------------------------

    companion object {
        fun newInstance(key: String): ResetToDefaultDialogPreferenceDialog {
            val fragment = ResetToDefaultDialogPreferenceDialog()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
