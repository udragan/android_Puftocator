package ns.fajnet.android.puftocatorprovider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import ns.fajnet.android.puftocatorprovider.common.Constants
import ns.fajnet.android.puftocatorprovider.common.LogEx

class MainActivity : AppCompatActivity() {

    // members ---------------------------------------------------------------------------------------------------------

    private val locationRequestInterval: Long = 100//60000           // 60 sec
    private val locationRequestFastestInterval: Long = 50//10000    // 10 sec
    private val locationRequestMaxWaitTime: Long = 1000//120000       // 120 sec
    private val locationRequestSmallestDisplacement = 50f      // 50 meters

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionGrants(permissions)
        }

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()
        setContentView(R.layout.activity_main)

        when {
            isPermissionGranted() -> when {
                isLocationEnabled() -> subscribeToLocationUpdates()
                else -> {
                    //Utils.showGPSNotEnabledDialog(this)
                }
            }
            else -> requestLocationPermission()
        }
    }

    // private methods -------------------------------------------------------------------------------------------------

    private fun initialize() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                LogEx.d(Constants.TAG_MAIN_ACTIVITY, "locationCallbackTriggered")

                for (location in locationResult.locations) {
                    // TODO: check the lowest api level where writing to realtimeDb works!!!

                    val database = Firebase.database
                    val myRef = database.getReference("message")

                    myRef.setValue(location)
                }
            }
        }
    }

    private fun isPermissionGranted(): Boolean {
        val permissionStatus = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val result = permissionStatus == PackageManager.PERMISSION_GRANTED
        LogEx.i(Constants.TAG_MAIN_ACTIVITY, "Location permission granted: $result")

        return result
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val result = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        LogEx.i(Constants.TAG_MAIN_ACTIVITY, "Location enabled: $result")

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
        LogEx.i(Constants.TAG_MAIN_ACTIVITY, "Location permission requested.")
    }

    private fun handlePermissionGrants(permissions: Map<String, Boolean>) {
        permissions.forEach { actionMap ->
            when (actionMap.key) {
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    if (actionMap.value) {
                        subscribeToLocationUpdates()
                        Log.i(Constants.TAG_MAIN_ACTIVITY, "Location permission granted.")
                    } else {
                        Log.i(Constants.TAG_MAIN_ACTIVITY, "Location permission denied.")
                        finishAffinity()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeToLocationUpdates() {
        Log.i(Constants.TAG_MAIN_ACTIVITY, "Subscribe to location updates.")
        Looper.myLooper()?.let {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            fusedLocationProviderClient.requestLocationUpdates(
                generateLocationRequest(),
                locationCallback,
                it
            )
        }
    }

    private fun generateLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = locationRequestInterval
            fastestInterval = locationRequestFastestInterval
            maxWaitTime = locationRequestMaxWaitTime
            smallestDisplacement = locationRequestSmallestDisplacement
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }
}
