package ns.fajnet.android.puftocatorclient.activities.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import ns.fajnet.android.puftocatorclient.R

class PreferenceLocationPassiveFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.location_passive_preferences, rootKey)
    }
}
