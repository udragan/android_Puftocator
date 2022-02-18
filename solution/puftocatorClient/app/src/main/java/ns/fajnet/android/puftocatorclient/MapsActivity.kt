package ns.fajnet.android.puftocatorclient

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx
import ns.fajnet.android.puftocatorclient.common.Utils
import ns.fajnet.android.puftocatorclient.databinding.ActivityMapsBinding
import ns.fajnet.android.puftocatorclient.services.GeoService

class MapsActivity : AppCompatActivity(), ServiceConnection, OnMapReadyCallback {

    // members ---------------------------------------------------------------------------------------------------------

    private val viewModel: MapsActivityViewModel by viewModels()

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionGrants(permissions)
        }
    private var hostMarker: Marker? = null
    private var targetMarker: Marker? = null
    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        viewModel.startService()
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, GeoService::class.java)
        bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        unbindService(this)
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
                val latLng = LatLng(it.latitude, it.longitude)

                if (targetMarker == null) {
                    targetMarker = map.addMarker(
                        MarkerOptions().position(latLng)
                            .flat(true)
                            .title("Target")
                    )
                }

                targetMarker?.position = latLng
                targetMarker?.rotation = 45f
                targetMarker?.isVisible = true

                val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                map.moveCamera(update)
            } else{
                targetMarker?.isVisible = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
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
