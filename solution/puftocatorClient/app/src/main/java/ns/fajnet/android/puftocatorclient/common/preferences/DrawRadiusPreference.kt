package ns.fajnet.android.puftocatorclient.common.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import ns.fajnet.android.puftocatorclient.R
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx

class DrawRadiusPreference(private val context: Context) : IPreference,
    SharedPreferences.OnSharedPreferenceChangeListener {

    // members ---------------------------------------------------------------------------------------------------------

    private var _value = false
    private val subscribers = mutableListOf<() -> Unit>()

    // init ------------------------------------------------------------------------------------------------------------

    init {
        readExistingPreference()
        registerPreferenceChangeListener()
    }

    // overrides -------------------------------------------------------------------------------------------------------

    override fun value(): Boolean {
        return _value
    }

    override fun dispose() {
        unregisterPreferenceChangeListener()
        unsubscribeAll()
    }

    // OnSharedPreferencesChangedListener ------------------------------------------------------------------------------

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == context.getString(R.string.settings_preference_draw_radius_key)) {
            LogEx.d(Constants.TAG_PREFERENCE_DRAW_RADIUS, "preference changed")
            _value = sharedPreferences
                .getBoolean(
                    context.getString(R.string.settings_preference_draw_radius_key),
                    Constants.PREFERENCE_DEFAULT_DRAW_RADIUS
                )

            if (subscribers.any()) {
                LogEx.d(
                    Constants.TAG_PREFERENCE_DRAW_RADIUS,
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
            .getBoolean(
                context.getString(R.string.settings_preference_draw_radius_key),
                Constants.PREFERENCE_DEFAULT_DRAW_RADIUS
            )
        LogEx.d(Constants.TAG_PREFERENCE_DRAW_RADIUS, "read current value: $_value")
    }

    private fun registerPreferenceChangeListener() {
        LogEx.d(Constants.TAG_PREFERENCE_DRAW_RADIUS, "register preference change listener")
        PreferenceManager.getDefaultSharedPreferences(context)
            .registerOnSharedPreferenceChangeListener(this)
    }

    private fun unregisterPreferenceChangeListener() {
        LogEx.d(Constants.TAG_PREFERENCE_DRAW_RADIUS, "unregister preference change listener")
        PreferenceManager.getDefaultSharedPreferences(context)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun unsubscribeAll() {
        subscribers.clear()
    }
}
