package ns.fajnet.android.puftocatorclient.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class LocationInfo(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var speed: Double = 0.0,

    var accuracy: Double = 0.0,
    var bearing: Double = 0.0,

    var elapsedRealtime: Double = 0.0,
    var time: Double = 0.0,
)
