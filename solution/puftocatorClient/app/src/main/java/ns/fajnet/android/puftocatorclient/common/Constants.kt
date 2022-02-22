package ns.fajnet.android.puftocatorclient.common

internal class Constants {
    companion object {

        // firebase value reference name -------------------------------------------------------------------------------

        const val FIREBASE_REFERENCE = "message"

        // geo service -------------------------------------------------------------------------------------------------

        const val NOTIFICATION_CHANNEL_ID_GEO_SERVICE = "geoNotificationChannel"
        const val NOTIFICATION_CHANNEL_NAME_GEO_SERVICE = "GEO Service Channel"
        const val NOTIFICATION_SERVICE_ID_GEO_SERVICE = 1000

        // logger tags -------------------------------------------------------------------------------------------------

        const val TAG_MAPS_ACTIVITY = "appTag_maps_activity"
        const val TAG_MAPS_ACTIVITY_VIEW_MODEL = "appTag_maps_activity_view_model"

        const val TAG_GEO_SERVICE = "appTag_geoService"

        const val TAG_PREFERENCE_TRIGGER_RADIUS = "appTag_preference_trigger_radius"
    }
}
