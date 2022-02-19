package ns.fajnet.android.puftocatorclient.models

import android.location.Location
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class LocationInfo(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var speed: Float = 0f,

    var accuracy: Float = 0f,
    var bearing: Float = 0f,

    var elapsedRealtime: Long = 0,
    var time: Long = 0
) {

    // public methods --------------------------------------------------------------------------------------------------

    fun toLocation(): Location {
        val location = Location("local")
        location.latitude = latitude
        location.longitude = longitude
        location.altitude = altitude
        location.speed = speed
        location.accuracy = accuracy
        location.bearing = bearing
        location.elapsedRealtimeNanos = elapsedRealtime
        location.time = time

        return location
    }

    // companion object ------------------------------------------------------------------------------------------------

    companion object {
        fun fromLocation(location: Location): LocationInfo {
            return LocationInfo(
                location.latitude,
                location.longitude,
                location.altitude,
                location.speed,
                location.accuracy,
                location.bearing,
                location.elapsedRealtimeNanos,
                location.time
            )
        }
    }
}
