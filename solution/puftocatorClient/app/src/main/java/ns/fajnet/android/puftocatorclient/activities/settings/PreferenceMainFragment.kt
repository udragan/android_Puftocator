package ns.fajnet.android.puftocatorclient.activities.settings

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import ns.fajnet.android.puftocatorclient.R
import ns.fajnet.android.puftocatorclient.common.preferences.ResetToDefaultDialogPreference
import ns.fajnet.android.puftocatorclient.common.preferences.ResetToDefaultDialogPreferenceDialog

class PreferenceMainFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        setFragmentResultListener("requestKey") { _, _ ->
            run {
                preferenceScreen = null
                addPreferencesFromResource(R.xml.root_preferences)
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ResetToDefaultDialogPreference) {

            val dialogFragment: DialogFragment = ResetToDefaultDialogPreferenceDialog.newInstance(preference.getKey())
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, null)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}
