package ns.fajnet.android.puftocatorclient

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx
import ns.fajnet.android.puftocatorclient.models.LocationInfo

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    // members ---------------------------------------------------------------------------------------------------------

    private lateinit var map: GoogleMap
    private var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var dbReference: DatabaseReference = database.getReference(Constants.FIREBASE_REFERENCE)

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionGrants(permissions)
        }

    private var targetMarker: Marker? = null

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        when {
            isPermissionGranted() -> when {
                isLocationEnabled() -> {
                    enableMyLocation()
                }
                else -> {
                    //Utils.showGPSNotEnabledDialog(this)
                }
            }
            else -> requestLocationPermission()
        }

        dbReference.addValueEventListener(locListener)
    }

    // private methods -------------------------------------------------------------------------------------------------

    private val locListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                val location = snapshot.getValue(LocationInfo::class.java)
                val locationLat = location?.latitude
                val locationLong = location?.longitude

                if (locationLat != null && locationLong != null) {
                    val latLng = LatLng(locationLat, locationLong)

                    if (targetMarker == null) {
                        targetMarker = map.addMarker(
                            MarkerOptions().position(latLng)
                                .flat(true)
                                .title("Target")
                        )
                    }

                    targetMarker?.position = latLng
                    targetMarker?.rotation = 45f

                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    map.moveCamera(update)
                } else {
                    LogEx.e(Constants.TAG_MAPS_ACTIVITY, "user location cannot be found")
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(applicationContext, "Could not read from database", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
    }

    private fun isPermissionGranted(): Boolean {
        val permissionStatus = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val result = permissionStatus == PackageManager.PERMISSION_GRANTED
        LogEx.i(Constants.TAG_MAPS_ACTIVITY, "Location permission granted: $result")

        return result
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val result = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        LogEx.i(Constants.TAG_MAPS_ACTIVITY, "Location enabled: $result")

        return result
    }

    private fun requestLocationPermission() {
        val permissionsToRequest =
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            }

        requestPermissions.launch(permissionsToRequest)
        LogEx.i(Constants.TAG_MAPS_ACTIVITY, "Location permission requested.")
    }

    private fun handlePermissionGrants(permissions: Map<String, Boolean>) {
        permissions.forEach { actionMap ->
            when (actionMap.key) {
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    if (actionMap.value) {
                        enableMyLocation()
                        LogEx.i(Constants.TAG_MAPS_ACTIVITY, "Location permission granted.")
                    } else {
                        LogEx.i(Constants.TAG_MAPS_ACTIVITY, "Location permission denied.")
                        finishAffinity()
                    }
                }
            }
        }
    }
}
