package ns.fajnet.android.puftocatorclient.activities.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import ns.fajnet.android.puftocatorclient.R
import ns.fajnet.android.puftocatorclient.activities.settings.SettingsActivity
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx
import ns.fajnet.android.puftocatorclient.common.Utils
import ns.fajnet.android.puftocatorclient.databinding.ActivityMainBinding
import ns.fajnet.android.puftocatorclient.models.LocationInfo
import ns.fajnet.android.puftocatorclient.services.GeoService

class MainActivity : AppCompatActivity(), ServiceConnection, OnMapReadyCallback {

    // members ---------------------------------------------------------------------------------------------------------

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionGrants(permissions)
        }
    private var targetMarker: Marker? = null
    private var targetAccuracy: Circle? = null
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        viewModel.startService()
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, GeoService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        unbindService(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_exit -> {
                val intent = Intent(this, GeoService::class.java)
                stopService(intent)
                finishAndRemoveTask()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        bindLiveData()

        when {
            Utils.isPermissionGranted(this) -> when {
                Utils.isLocationEnabled(this) -> {
                    enableMyLocation()
                }
                else -> {
                    //Utils.showGPSNotEnabledDialog(this)
                }
            }
            else -> requestLocationPermission()
        }
    }

    // ServiceConnection -----------------------------------------------------------------------------------------------

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as GeoService.MyBinder
        LogEx.d(Constants.TAG_MAPS_ACTIVITY, "connected to service")
        viewModel.setGeoService(binder.service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        LogEx.d(Constants.TAG_MAPS_ACTIVITY, "disconnected from service")
    }

    // private methods -------------------------------------------------------------------------------------------------

    private fun bindLiveData() {
        viewModel.liveTargetLocation.observe(this) {
            if (it != null) {
                setTargetMarker(it)
            } else {
                clearTargetMarker()
            }
        }
    }

    private fun setTargetMarker(locationInfo: LocationInfo) {
        val latLon = LatLng(locationInfo.latitude, locationInfo.longitude)

        if (targetMarker == null) {
            targetMarker = map.addMarker(
                MarkerOptions()
                    .position(latLon)
                    .flat(true)
                    .title("Target")
            )
        }

        targetMarker?.position = latLon
        targetMarker?.rotation = locationInfo.bearing
        targetMarker?.isVisible = true

        if (targetAccuracy == null) {
            targetAccuracy = map.addCircle(
                CircleOptions()
                    .center(latLon)
                    .strokeWidth(5f)
                    .strokeColor(getColor(R.color.marker_radius_stroke))
                    .fillColor(getColor(R.color.marker_radius_fill))
            )
        }

        targetAccuracy?.center = latLon
        targetAccuracy?.radius = locationInfo.accuracy.toDouble()
        targetAccuracy?.isVisible = true

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLon, 16.0f))
    }

    private fun clearTargetMarker() {
        targetAccuracy?.isVisible = false
        targetMarker?.isVisible = false
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f))
            }
        }
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
