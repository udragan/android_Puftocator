package ns.fajnet.android.puftocatorclient.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

object Utils {

    fun isPermissionGranted(context: Context): Boolean {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val result = permissionStatus == PackageManager.PERMISSION_GRANTED
        LogEx.i(Constants.TAG_MAPS_ACTIVITY, "Location permission granted: $result")

        return result
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager: LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val result = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        LogEx.i(Constants.TAG_MAPS_ACTIVITY, "Location enabled: $result")

        return result
    }
}
