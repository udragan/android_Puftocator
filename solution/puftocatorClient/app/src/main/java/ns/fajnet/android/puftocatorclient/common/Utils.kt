package ns.fajnet.android.puftocatorclient.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object Utils {

    // permissions -----------------------------------------------------------------------------------------------------

    fun isPermissionGranted(context: Context): Boolean {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val result = permissionStatus == PackageManager.PERMISSION_GRANTED
        LogEx.i(Constants.TAG_UTILS, "Location permission granted: $result")

        return result
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager: LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val result = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        LogEx.i(Constants.TAG_UTILS, "Location enabled: $result")

        return result
    }

    // graphics --------------------------------------------------------------------------------------------------------

    fun bitmapDescriptorFromVector(context: Context, resId: Int, hue: Float): BitmapDescriptor {
        val vectorDrawable = ResourcesCompat.getDrawable(context.resources, resId, null)
            ?: return BitmapDescriptorFactory.defaultMarker(hue)

        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val color = Color.HSVToColor(floatArrayOf(hue, 255f, 255f))
        val bitmap =
            Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        DrawableCompat.setTint(vectorDrawable, color)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
