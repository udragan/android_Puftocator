package ns.fajnet.android.puftocatorclient.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.firebase.database.*
import kotlinx.coroutines.*
import ns.fajnet.android.puftocatorclient.R
import ns.fajnet.android.puftocatorclient.activities.main.MainActivity
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx
import ns.fajnet.android.puftocatorclient.common.Utils
import ns.fajnet.android.puftocatorclient.models.LocationInfo

class GeoService : Service() {

    // members ---------------------------------------------------------------------------------------------------------

    // TODO: move location parameters to settings
    private val locationRequestInterval: Long = 100//60000           // 60 sec
    private val locationRequestFastestInterval: Long = 50//10000    // 10 sec
    private val locationRequestMaxWaitTime: Long = 1000//120000       // 120 sec
    private val locationRequestSmallestDisplacement = 50f      // 50 meters

    private val _liveTargetLocation = MutableLiveData<LocationInfo?>()

    private val mBinder: IBinder = MyBinder()

    private var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var dbReference: DatabaseReference = database.getReference(Constants.FIREBASE_REFERENCE)

    private lateinit var serviceScope: CoroutineScope
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var firebaseListener: ValueEventListener
    private lateinit var hostLocation: Location
    private lateinit var targetLocation: Location

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        LogEx.d(Constants.TAG_GEO_SERVICE, "onCreate")
        initialize()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogEx.d(Constants.TAG_GEO_SERVICE, "onStartCommand")

        if (!isServiceRunningInForeground(this, GeoService::class.java)) {
            startForeground(Constants.NOTIFICATION_SERVICE_ID_GEO_SERVICE, generateNotification())

            if (checkPrerequisites()) {
                subscribeToLocationUpdates()
                subscribeToFirebaseUpdates()
            } else {
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        LogEx.d(Constants.TAG_GEO_SERVICE, "onDestroy")
        unsubscribeFromFirebaseUpdates()
        unsubscribeFromLocationUpdates()
        serviceScope.cancel()
    }

    // properties ------------------------------------------------------------------------------------------------------

    val liveTargetLocation: LiveData<LocationInfo?>
        get() = _liveTargetLocation

    // private methods -------------------------------------------------------------------------------------------------

    private fun initialize() {
        serviceScope = CoroutineScope(Dispatchers.IO)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                LogEx.d(Constants.TAG_GEO_SERVICE, "locationCallbackTriggered")
                super.onLocationResult(locationResult)

                serviceScope.launch {
                    for (location in locationResult.locations) {
                        LogEx.d(Constants.TAG_GEO_SERVICE, "location received: $location")
                        hostLocation = location
                        calculateThreat()
                        LogEx.d(Constants.TAG_GEO_SERVICE, "track update published")
                    }
                }
            }
        }

        firebaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                LogEx.d(Constants.TAG_GEO_SERVICE, "firebaseCallbackTriggered")

                serviceScope.launch {
                    if (snapshot.exists()) {
                        val location = snapshot.getValue(LocationInfo::class.java)

                        if (location != null) {
                            LogEx.d(Constants.TAG_GEO_SERVICE, "firebase location received: $location")
                            targetLocation = location.toLocation()
                        } else {
                            LogEx.e(Constants.TAG_MAPS_ACTIVITY, "firebase location cannot be found")
                        }

                        calculateThreat()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Could not read from database", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkPrerequisites(): Boolean {
        LogEx.d(
            Constants.TAG_GEO_SERVICE,
            "hasPermission: ${Utils.isPermissionGranted(this)}, locationEnabled: ${Utils.isLocationEnabled(this)}"
        )

        return Utils.isPermissionGranted(this) && Utils.isLocationEnabled(this)
    }

    private fun generateNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID_GEO_SERVICE)
            .setContentTitle(getString(R.string.geo_service_notification_title))
            .setContentText(getString(R.string.geo_service_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun generateNotification(content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID_GEO_SERVICE)
            .setContentTitle(getString(R.string.geo_service_notification_title))
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setLargeIcon(BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_myplaces))
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(false)
            .setColor(ContextCompat.getColor(this, R.color.teal_700))
            .setColorized(true)
            .build()
    }

    private fun generateSilentNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID_GEO_SERVICE)
            .setContentTitle(getString(R.string.geo_service_notification_title))
            .setContentText(getString(R.string.geo_service_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunningInForeground(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return service.foreground
            }
        }

        return false
    }

    private fun subscribeToFirebaseUpdates() {
        dbReference.addValueEventListener(firebaseListener)
    }

    private fun unsubscribeFromFirebaseUpdates() {
        dbReference.removeEventListener(firebaseListener)
    }

    @SuppressLint("MissingPermission")
    private fun subscribeToLocationUpdates() {
        LogEx.d(Constants.TAG_GEO_SERVICE, "subscribe to location updates")

        serviceScope.launch {
            withContext(Dispatchers.Main) {
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
                Looper.myLooper()?.let {
                    fusedLocationProviderClient.requestLocationUpdates(
                        generateLocationRequest(),
                        locationCallback,
                        it
                    )
                }
            }
        }
    }

    private fun unsubscribeFromLocationUpdates() {

        if (this::fusedLocationProviderClient.isInitialized) {
            LogEx.d(Constants.TAG_GEO_SERVICE, "unsubscribe from location updates")
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun generateLocationRequest(): LocationRequest {
        //val params = retrieveLocationSubscriptionParametersFromPreferences()

        return LocationRequest.create().apply {
            interval = locationRequestInterval
            fastestInterval = locationRequestFastestInterval
            maxWaitTime = locationRequestMaxWaitTime
            smallestDisplacement = locationRequestSmallestDisplacement
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun calculateThreat() {
        if (!this::hostLocation.isInitialized || !this::targetLocation.isInitialized) {
            _liveTargetLocation.postValue(null)
            return
        }

        val distance = hostLocation.distanceTo(targetLocation)

        // TODO: read distance from settings (in meters)
        if (distance < 100) {
            // TODO: publish notification
            NotificationManagerCompat.from(this)
                .notify(Constants.NOTIFICATION_SERVICE_ID_GEO_SERVICE, generateNotification(distance.toString()))
            _liveTargetLocation.postValue(LocationInfo.fromLocation(targetLocation))
        } else {
            // TODO: publish silent notification
            NotificationManagerCompat.from(this)
                .notify(Constants.NOTIFICATION_SERVICE_ID_GEO_SERVICE, generateSilentNotification())
            _liveTargetLocation.postValue(null)
        }
    }


    // inner classes ---------------------------------------------------------------------------------------------------

    inner class MyBinder : Binder() {
        val service: GeoService
            get() = this@GeoService
    }
}
