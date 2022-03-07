package ns.fajnet.android.puftocatorclient.common.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import ns.fajnet.android.puftocatorclient.R
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx

class TriggerRadiusPreference(private val context: Context) : IPreference,
    SharedPreferences.OnSharedPreferenceChangeListener {

    // members ---------------------------------------------------------------------------------------------------------

    private var _value = ""
    private val subscribers = mutableListOf<() -> Unit>()

    // init ------------------------------------------------------------------------------------------------------------

    init {
        readExistingPreference()
        registerPreferenceChangeListener()
    }

    // overrides -------------------------------------------------------------------------------------------------------

    override fun value(): Int {
        return _value.toInt()
    }

    override fun dispose() {
        unregisterPreferenceChangeListener()
        unsubscribeAll()
    }

    // OnSharedPreferencesChangedListener ------------------------------------------------------------------------------

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == context.getString(R.string.settings_preference_radius_key)) {
            LogEx.d(Constants.TAG_PREFERENCE_TRIGGER_RADIUS, "preference changed")
            val defaultValue = context.resources.getStringArray(R.array.radius_values)[2]
            _value = sharedPreferences
                .getString(
                    context.getString(R.string.settings_preference_radius_key),
                    defaultValue
                )!!

            if (subscribers.any()) {
                LogEx.d(
                    Constants.TAG_PREFERENCE_TRIGGER_RADIUS,
                    "triggering ${subscribers.size} subscribers"
                )
                subscribers.forEach { x -> x.invoke() }
            }
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
        val defaultValue =
            context.resources.getStringArray(R.array.radius_values)[2]
        _value = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(
                context.getString(R.string.settings_preference_radius_key),
                defaultValue
            )!!
        LogEx.d(Constants.TAG_PREFERENCE_TRIGGER_RADIUS, "read current value: $_value")
    }

    private fun registerPreferenceChangeListener() {
        LogEx.d(Constants.TAG_PREFERENCE_TRIGGER_RADIUS, "register preference change listener")
        PreferenceManager.getDefaultSharedPreferences(context)
            .registerOnSharedPreferenceChangeListener(this)
    }

    private fun unregisterPreferenceChangeListener() {
        LogEx.d(Constants.TAG_PREFERENCE_TRIGGER_RADIUS, "unregister preference change listener")
        PreferenceManager.getDefaultSharedPreferences(context)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun unsubscribeAll() {
        subscribers.clear()
    }
}
