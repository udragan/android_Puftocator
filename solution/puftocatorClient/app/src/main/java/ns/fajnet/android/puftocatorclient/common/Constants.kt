package ns.fajnet.android.puftocatorclient.common

internal class Constants {
    companion object {

        // firebase value reference name -------------------------------------------------------------------------------

        const val FIREBASE_REFERENCE = "message"

        // geo service -------------------------------------------------------------------------------------------------

        const val NOTIFICATION_CHANNEL_ID_GEO_SERVICE = "geoNotificationChannel"
        const val NOTIFICATION_CHANNEL_NAME_GEO_SERVICE = "GEO Service Channel"
        const val NOTIFICATION_SERVICE_ID_GEO_SERVICE = 1000

        // preference default values -----------------------------------------------------------------------------------

        const val PREFERENCE_DEFAULT_ACTIVE_REQUEST_INTERVAL = 2
        const val PREFERENCE_DEFAULT_ACTIVE_REQUEST_FASTEST_INTERVAL = 1
        const val PREFERENCE_DEFAULT_ACTIVE_MAX_WAIT_TIME = 120
        const val PREFERENCE_DEFAULT_ACTIVE_SMALLEST_DISPLACEMENT = 50
        const val PREFERENCE_DEFAULT_PASSIVE_REQUEST_INTERVAL = 60
        const val PREFERENCE_DEFAULT_PASSIVE_REQUEST_FASTEST_INTERVAL = 30
        const val PREFERENCE_DEFAULT_PASSIVE_MAX_WAIT_TIME = 300
        const val PREFERENCE_DEFAULT_PASSIVE_SMALLEST_DISPLACEMENT = 200
        const val PREFERENCE_DEFAULT_DRAW_RADIUS = false

        // logger tags -------------------------------------------------------------------------------------------------

        const val TAG_UTILS = "appTag_utils"
        const val TAG_MAIN_ACTIVITY = "appTag_main_activity"
        const val TAG_MAIN_ACTIVITY_VIEW_MODEL = "appTag_main_activity_view_model"

        const val TAG_GEO_SERVICE = "appTag_geoService"

        const val TAG_PREFERENCE_ACTIVE_REQUEST_INTERVAL = "appTag_preference_active_request_interval"
        const val TAG_PREFERENCE_ACTIVE_REQUEST_FASTEST_INTERVAL = "appTag_preference_active_request_fastest_interval"
        const val TAG_PREFERENCE_ACTIVE_MAX_WAIT_TIME = "appTag_preference_active_max_wait_time"
        const val TAG_PREFERENCE_ACTIVE_SMALLEST_DISPLACEMENT = "appTag_preference_active_smallest_displacement"
        const val TAG_PREFERENCE_PASSIVE_REQUEST_INTERVAL = "appTag_preference_passive_request_interval"
        const val TAG_PREFERENCE_PASSIVE_REQUEST_FASTEST_INTERVAL = "appTag_preference_passive_request_fastest_interval"
        const val TAG_PREFERENCE_PASSIVE_MAX_WAIT_TIME = "appTag_preference_passive_max_wait_time"
        const val TAG_PREFERENCE_PASSIVE_SMALLEST_DISPLACEMENT = "appTag_preference_passive_smallest_displacement"
        const val TAG_PREFERENCE_TRIGGER_RADIUS = "appTag_preference_trigger_radius"
        const val TAG_PREFERENCE_DRAW_RADIUS = "appTag_preference_draw_radius"
    }
}
