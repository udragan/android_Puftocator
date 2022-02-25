package ns.fajnet.android.puftocatorclient.common.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import ns.fajnet.android.puftocatorclient.R
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx

class ActiveSmallestDisplacementPreference(private val context: Context) : IPreference,
    SharedPreferences.OnSharedPreferenceChangeListener {

    // members ---------------------------------------------------------------------------------------------------------

    private var _value = Constants.PREFERENCE_DEFAULT_ACTIVE_SMALLEST_DISPLACEMENT
    private val subscribers = mutableListOf<() -> Unit>()

    // init ------------------------------------------------------------------------------------------------------------

    init {
        readExistingPreference()
        registerPreferenceChangeListener()
    }

    // overrides -------------------------------------------------------------------------------------------------------

    override fun value(): Float {
        return _value.toFloat()
    }

    override fun dispose() {
        unregisterPreferenceChangeListener()
        unsubscribeAll()
    }

    // OnSharedPreferencesChangedListener ------------------------------------------------------------------------------

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == context.getString(R.string.settings_preference_location_active_smallest_displacement_key)) {
            LogEx.d(Constants.TAG_PREFERENCE_ACTIVE_SMALLEST_DISPLACEMENT, "preference changed")
            _value = sharedPreferences
                .getInt(
                    context.getString(R.string.settings_preference_location_active_smallest_displacement_key),
                    Constants.PREFERENCE_DEFAULT_ACTIVE_SMALLEST_DISPLACEMENT
                )

            LogEx.d(
                Constants.TAG_PREFERENCE_ACTIVE_SMALLEST_DISPLACEMENT,
                "triggering ${subscribers.size} subscribers"
            )
            subscribers.forEach { x -> x.invoke() }
        }
    }

    // public methods --------------------------------------------------------------------------------------------------

    fun subscribe(action: () -> Unit) {
        subscribers.add(action)
    }

    fun unsubscribe(action: () -> Unit) {
        subscribers.remove(action)
    }

    // private methods -------------------------------------------------------------------------------------------------

    private fun readExistingPreference() {
        _value = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(
                context.getString(R.string.settings_preference_location_active_smallest_displacement_key),
                Constants.PREFERENCE_DEFAULT_ACTIVE_SMALLEST_DISPLACEMENT
            )
        LogEx.d(Constants.TAG_PREFERENCE_ACTIVE_SMALLEST_DISPLACEMENT, "read current value: $_value")
    }

    private fun registerPreferenceChangeListener() {
        LogEx.d(Constants.TAG_PREFERENCE_ACTIVE_SMALLEST_DISPLACEMENT, "register preference change listener")
        PreferenceManager.getDefaultSharedPreferences(context)
            .registerOnSharedPreferenceChangeListener(this)
    }

    private fun unregisterPreferenceChangeListener() {
        LogEx.d(Constants.TAG_PREFERENCE_ACTIVE_SMALLEST_DISPLACEMENT, "unregister preference change listener")
        PreferenceManager.getDefaultSharedPreferences(context)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun unsubscribeAll() {
        subscribers.clear()
    }
}
