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
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.SphericalUtil
import ns.fajnet.android.puftocatorclient.R
import ns.fajnet.android.puftocatorclient.activities.settings.SettingsActivity
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx
import ns.fajnet.android.puftocatorclient.common.Utils
import ns.fajnet.android.puftocatorclient.common.Utils.bitmapDescriptorFromVector
import ns.fajnet.android.puftocatorclient.common.preferences.*
import ns.fajnet.android.puftocatorclient.databinding.ActivityMainBinding
import ns.fajnet.android.puftocatorclient.models.LocationInfo
import ns.fajnet.android.puftocatorclient.services.GeoService

class MainActivity : AppCompatActivity(),
    ServiceConnection,
    OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnCameraMoveStartedListener {

    // members ---------------------------------------------------------------------------------------------------------

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionGrants(permissions)
        }
    private var myLocationRadius: Circle? = null
    private var targetMarker: Marker? = null
    private var targetMarkerAccuracyRadius: Circle? = null
    private var followMyLocation: Boolean = true
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var activeRequestIntervalPref: ActiveRequestIntervalPreference
    private lateinit var activeRequestFastestIntervalPref: ActiveRequestFastestIntervalPreference
    private lateinit var activeMaxWaitPref: ActiveMaxWaitPreference
    private lateinit var triggerRadiusPref: TriggerRadiusPreference
    private lateinit var drawRadiusPref: DrawRadiusPreference
    private lateinit var snackBar: Snackbar

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        LogEx.d(Constants.TAG_MAIN_ACTIVITY, "onCreate")
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
        LogEx.d(Constants.TAG_MAIN_ACTIVITY, "onDestroy")
        super.onDestroy()
        unsubscribe()
        triggerRadiusPref.dispose()
        drawRadiusPref.dispose()
        activeRequestIntervalPref.dispose()
        activeRequestFastestIntervalPref.dispose()
        activeMaxWaitPref.dispose()
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
        LogEx.d(Constants.TAG_MAIN_ACTIVITY, "onMapReady")
        map = googleMap
        with(googleMap) {
            uiSettings.isZoomControlsEnabled = true
            setOnMyLocationButtonClickListener(this@MainActivity)
            setOnMapClickListener(this@MainActivity)
            setOnCameraMoveStartedListener(this@MainActivity)
        }

        initializeMarkers()

        bindLiveData()
        subscribe()

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

    override fun onCameraMoveStarted(reason: Int) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            followMyLocation = false
        }
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
        LogEx.d(Constants.TAG_MAIN_ACTIVITY, "initialize")
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                LogEx.d(Constants.TAG_MAIN_ACTIVITY, "locationCallbackTriggered")
                super.onLocationResult(locationResult)

                // TODO: move all processing to background thread!
                //serviceScope.launch {
                for (location in locationResult.locations) {
                    LogEx.d(Constants.TAG_MAIN_ACTIVITY, "location received: $location")
                    drawMyLocationRadius(location)
                    LogEx.d(Constants.TAG_MAIN_ACTIVITY, "location update published")
                }
                //}
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        activeRequestIntervalPref = ActiveRequestIntervalPreference(applicationContext)
        activeRequestFastestIntervalPref = ActiveRequestFastestIntervalPreference(applicationContext)
        activeMaxWaitPref = ActiveMaxWaitPreference(applicationContext)
        triggerRadiusPref = TriggerRadiusPreference(applicationContext)
        drawRadiusPref = DrawRadiusPreference(applicationContext)
        snackBar = Snackbar.make(binding.root, R.string.main_activity_message_no_internet, Snackbar.LENGTH_INDEFINITE)

        binding.btnFindLocation.setOnClickListener {
            // TEST
        }
    }

    private fun initializeMarkers() {
        LogEx.d(Constants.TAG_MAIN_ACTIVITY, "initialize markers")
        targetMarker = map.addMarker(
            MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .anchor(0.5f, 0.5f)
                .flat(true)
                .icon(
                    bitmapDescriptorFromVector(
                        this,
                        R.drawable.ic_location_dot_directional_24,
                        BitmapDescriptorFactory.HUE_RED
                    )
                )
                .title("Target")
                .visible(false)
        )
        targetMarkerAccuracyRadius = map.addCircle(
            CircleOptions()
                .center(LatLng(0.0, 0.0))
                .strokeWidth(5f)
                .strokeColor(getColor(R.color.marker_radius_stroke))
                .fillColor(getColor(R.color.marker_radius_fill))
                .visible(false)
        )
        myLocationRadius = map.addCircle(
            CircleOptions()
                .center(LatLng(0.0, 0.0))
                .strokeWidth(5f)
                .strokeColor(getColor(R.color.marker_radius_stroke))
                .fillColor(getColor(R.color.marker_radius_fill))
        )
    }

    private fun bindLiveData() {
        viewModel.hasInternetConnection.observe(this) {
            if (it) {
                snackBar.dismiss()
            } else {
                snackBar.show()
            }
        }
        viewModel.liveTargetLocation.observe(this) {
            drawTargetMarker(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribe() {
        LogEx.d(Constants.TAG_MAIN_ACTIVITY, "subscribe to location updates")
        Looper.myLooper()?.let {
            fusedLocationClient.requestLocationUpdates(
                generateLocationRequest(),
                locationCallback,
                it
            )
        }
        triggerRadiusPref.subscribe {
            drawMyLocationRadius(Location("").apply {
                latitude = myLocationRadius?.center?.latitude!!
                longitude = myLocationRadius?.center?.longitude!!
            })
        }
        drawRadiusPref.subscribe {
            drawMyLocationRadius(Location("").apply {
                latitude = myLocationRadius?.center?.latitude!!
                longitude = myLocationRadius?.center?.longitude!!
            })
        }
    }

    private fun unsubscribe() {
        if (this::fusedLocationClient.isInitialized) {
            LogEx.d(Constants.TAG_MAIN_ACTIVITY, "unsubscribe from location updates")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun drawTargetMarker(location: LocationInfo?) {
        if (location != null) {
            val latLon = LatLng(location.latitude, location.longitude)

            targetMarker?.position = latLon
            targetMarker?.rotation = location.bearing
            targetMarker?.isVisible = true
            targetMarkerAccuracyRadius?.center = latLon
            targetMarkerAccuracyRadius?.radius = location.accuracy.toDouble()
            targetMarkerAccuracyRadius?.isVisible = true
        } else {
            targetMarkerAccuracyRadius?.isVisible = false
            targetMarker?.isVisible = false
        }
    }

    private fun drawMyLocationRadius(location: Location) {
        LogEx.d(Constants.TAG_MAIN_ACTIVITY, "drawMyLocationRadius - DrawRadiusPref = ${drawRadiusPref.value()}")
        val latLon = LatLng(location.latitude, location.longitude)
        myLocationRadius?.center = latLon
        myLocationRadius?.radius = triggerRadiusPref.value().toDouble()

        if (drawRadiusPref.value()) {
            myLocationRadius?.isVisible = true

            if (followMyLocation) {
                zoomToMyLocationRadius()
            }
        } else {
            myLocationRadius?.isVisible = false
        }
    }

    private fun zoomToMyLocationRadius() {
        val ne = SphericalUtil.computeOffset(myLocationRadius?.center, triggerRadiusPref.value().toDouble(), 90.0)
        val sw = SphericalUtil.computeOffset(myLocationRadius?.center, triggerRadiusPref.value().toDouble(), 270.0)

        val bounds = LatLngBounds(sw, ne)
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10))
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        map.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude)))
                zoomToMyLocationRadius()
            }
        }
    }

    private fun generateLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = activeRequestIntervalPref.value() * 1000
            fastestInterval = activeRequestFastestIntervalPref.value() * 1000
            maxWaitTime = activeMaxWaitPref.value() * 1000
            smallestDisplacement = 2f
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
