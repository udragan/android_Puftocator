package ns.fajnet.android.puftocatorclient.common.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import ns.fajnet.android.puftocatorclient.R
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx

class PassiveMaxWaitPreference(private val context: Context) : IPreference,
    SharedPreferences.OnSharedPreferenceChangeListener {

    // members ---------------------------------------------------------------------------------------------------------

    private var _value = Constants.PREFERENCE_DEFAULT_PASSIVE_MAX_WAIT_TIME
    private val subscribers = mutableListOf<() -> Unit>()

    // init ------------------------------------------------------------------------------------------------------------

    init {
        readExistingPreference()
        registerPreferenceChangeListener()
    }

    // overrides -------------------------------------------------------------------------------------------------------

    override fun value(): Long {
        return _value.toLong()
    }

    override fun dispose() {
        unregisterPreferenceChangeListener()
        unsubscribeAll()
    }

    // OnSharedPreferencesChangedListener ------------------------------------------------------------------------------

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == context.getString(R.string.settings_preference_location_passive_max_wait_time_key)) {
            LogEx.d(Constants.TAG_PREFERENCE_PASSIVE_MAX_WAIT_TIME, "preference changed")
            _value = sharedPreferences
                .getInt(
                    context.getString(R.string.settings_preference_location_passive_max_wait_time_key),
                    Constants.PREFERENCE_DEFAULT_PASSIVE_MAX_WAIT_TIME
                )

            if (subscribers.any()) {
                LogEx.d(
                    Constants.TAG_PREFERENCE_PASSIVE_MAX_WAIT_TIME,
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
        _value = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(
                context.getString(R.string.settings_preference_location_passive_max_wait_time_key),
                Constants.PREFERENCE_DEFAULT_PASSIVE_MAX_WAIT_TIME
            )
        LogEx.d(Constants.TAG_PREFERENCE_PASSIVE_MAX_WAIT_TIME, "read current value: $_value")
    }

    private fun registerPreferenceChangeListener() {
        LogEx.d(Constants.TAG_PREFERENCE_PASSIVE_MAX_WAIT_TIME, "register preference change listener")
        PreferenceManager.getDefaultSharedPreferences(context)
            .registerOnSharedPreferenceChangeListener(this)
    }

    private fun unregisterPreferenceChangeListener() {
        LogEx.d(Constants.TAG_PREFERENCE_PASSIVE_MAX_WAIT_TIME, "unregister preference change listener")
        PreferenceManager.getDefaultSharedPreferences(context)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun unsubscribeAll() {
        subscribers.clear()
    }
}
