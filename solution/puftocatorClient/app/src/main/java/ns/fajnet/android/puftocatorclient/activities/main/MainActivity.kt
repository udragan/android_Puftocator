package ns.fajnet.android.puftocatorclient.activities.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import ns.fajnet.android.puftocatorclient.R
import ns.fajnet.android.puftocatorclient.activities.settings.SettingsActivity
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx
import ns.fajnet.android.puftocatorclient.common.Utils
import ns.fajnet.android.puftocatorclient.common.preferences.*
import ns.fajnet.android.puftocatorclient.databinding.ActivityMainBinding
import ns.fajnet.android.puftocatorclient.models.LocationInfo
import ns.fajnet.android.puftocatorclient.services.GeoService

class MainActivity : AppCompatActivity(), ServiceConnection, OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMapClickListener {

    // members ---------------------------------------------------------------------------------------------------------

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionGrants(permissions)
        }
    private var hostRadius: Circle? = null
    private var targetMarker: Marker? = null
    private var targetAccuracy: Circle? = null
    private var followMyLocation: Boolean = false
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var activeRequestIntervalPreference: ActiveRequestIntervalPreference
    private lateinit var activeRequestFastestIntervalPreference: ActiveRequestFastestIntervalPreference
    private lateinit var activeMaxWaitPreference: ActiveMaxWaitPreference
    private lateinit var triggerRadiusPreference: TriggerRadiusPreference
    private lateinit var drawRadiusPreference: DrawRadiusPreference

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initialize()
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

    override fun onDestroy() {
        super.onDestroy()
        triggerRadiusPreference.dispose()
        drawRadiusPreference.dispose()
        activeRequestIntervalPreference.dispose()
        activeRequestFastestIntervalPreference.dispose()
        activeMaxWaitPreference.dispose()
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
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMapClickListener(this)
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

    override fun onMyLocationButtonClick(): Boolean {
        followMyLocation = true
        zoomToMyLocationRadius()
        return true
    }

    override fun onMapClick(point: LatLng) {
        followMyLocation = false
    }

    // ServiceConnection -----------------------------------------------------------------------------------------------

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as GeoService.MyBinder
        LogEx.d(Constants.TAG_MAIN_ACTIVITY, "connected to service")
        viewModel.setGeoService(binder.service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        LogEx.d(Constants.TAG_MAIN_ACTIVITY, "disconnected from service")
    }

    // private methods -------------------------------------------------------------------------------------------------

    private fun initialize() {
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                LogEx.d(Constants.TAG_MAIN_ACTIVITY, "locationCallbackTriggered")
                super.onLocationResult(locationResult)

                // TODO: move all processing to background thread!
                //serviceScope.launch {
                for (location in locationResult.locations) {
                    LogEx.d(Constants.TAG_MAIN_ACTIVITY, "location received: $location")
                    drawRadius(location)
                    LogEx.d(Constants.TAG_MAIN_ACTIVITY, "location update published")
                }
                //}
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        activeRequestIntervalPreference = ActiveRequestIntervalPreference(applicationContext)
        activeRequestFastestIntervalPreference = ActiveRequestFastestIntervalPreference(applicationContext)
        activeMaxWaitPreference = ActiveMaxWaitPreference(applicationContext)
        triggerRadiusPreference = TriggerRadiusPreference(applicationContext)
        drawRadiusPreference = DrawRadiusPreference(applicationContext)
    }

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

    // TODO: redraw on preference change (both radius and draw)
    private fun drawRadius(location: Location) {
        if (drawRadiusPreference.value()) {
            val latLon = LatLng(location.latitude, location.longitude)

            if (hostRadius == null) {
                hostRadius = map.addCircle(
                    CircleOptions()
                        .center(latLon)
                        .strokeWidth(5f)
                        .strokeColor(getColor(R.color.marker_radius_stroke))
                        .fillColor(getColor(R.color.marker_radius_fill))
                )
            }

            hostRadius?.center = latLon
            hostRadius?.radius = triggerRadiusPreference.value().toDouble()
            hostRadius?.isVisible = true

            if (followMyLocation) {
                zoomToMyLocationRadius()
            }
        }
    }

    private fun clearRadius() {
        hostRadius?.isVisible = false
    }

    private fun zoomToMyLocationRadius() {
        val ne = SphericalUtil.computeOffset(hostRadius?.center, triggerRadiusPreference.value().toDouble(), 90.0)
        val sw = SphericalUtil.computeOffset(hostRadius?.center, triggerRadiusPreference.value().toDouble(), 270.0)

        val bounds = LatLngBounds(sw, ne)
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10))
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        map.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f))
            }
        }
        Looper.myLooper()?.let {
            fusedLocationClient.requestLocationUpdates(
                generateLocationRequest(),
                locationCallback,
                it
            )
        }
    }

    private fun generateLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = activeRequestIntervalPreference.value() * 1000
            fastestInterval = activeRequestFastestIntervalPreference.value() * 1000
            maxWaitTime = activeMaxWaitPreference.value() * 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
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
        LogEx.i(Constants.TAG_MAIN_ACTIVITY, "Location permission requested.")
    }

    private fun handlePermissionGrants(permissions: Map<String, Boolean>) {
        permissions.forEach { actionMap ->
            when (actionMap.key) {
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    if (actionMap.value) {
                        enableMyLocation()
                        LogEx.i(Constants.TAG_MAIN_ACTIVITY, "Location permission granted.")
                    } else {
                        LogEx.i(Constants.TAG_MAIN_ACTIVITY, "Location permission denied.")
                        finishAffinity()
                    }
                }
            }
        }
    }
}
