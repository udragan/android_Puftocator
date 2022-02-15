package ns.fajnet.android.puftocatorclient.services

import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import ns.fajnet.android.puftocatorclient.MapsActivity
import ns.fajnet.android.puftocatorclient.R
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx
import ns.fajnet.android.puftocatorclient.common.Utils
import ns.fajnet.android.puftocatorclient.models.LocationInfo


class GeoService : Service() {

    // members ---------------------------------------------------------------------------------------------------------

    private val mBinder: IBinder = MyBinder()

    private var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var dbReference: DatabaseReference = database.getReference(Constants.FIREBASE_REFERENCE)

    private lateinit var serviceScope: CoroutineScope
    private lateinit var locListener : ValueEventListener

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        LogEx.d(Constants.TAG_GEO_SERVICE, "onCreate")
        initialize()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogEx.d(Constants.TAG_GEO_SERVICE, "onStartCommand")

        if (!isServiceRunningInForeground(this, GeoService::class.java)) {
            startForeground(Constants.SERVICE_ID_GEO_SERVICE, generateNotification())

            if (checkPrerequisites()) {
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
        serviceScope.cancel()
    }

    // private methods -------------------------------------------------------------------------------------------------

    private fun initialize() {
        serviceScope = CoroutineScope(Dispatchers.IO)

        locListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val location = snapshot.getValue(LocationInfo::class.java)
                    val locationLat = location?.latitude
                    val locationLong = location?.longitude

                    if (locationLat != null && locationLong != null) {
                        // TODO: publish targetLiveData!
                    } else {
                        LogEx.e(Constants.TAG_MAPS_ACTIVITY, "user location cannot be found")
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
            "hasPermission: ${Utils.isPermissionGranted(this)}, locationEnabled: ${
                Utils.isLocationEnabled(this)
            }"
        )

        return Utils.isPermissionGranted(this) && Utils.isLocationEnabled(this)
    }

    private fun generateNotification(): Notification {
        val notificationIntent = Intent(this, MapsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID_GEO_SERVICE)
            .setContentTitle(getString(R.string.geo_service_notification_title))
            .setContentText(getString(R.string.geo_service_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
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
        dbReference.addValueEventListener(locListener)

    }

    private fun unsubscribeFromFirebaseUpdates() {
        dbReference.removeEventListener(locListener)
    }


    // inner classes ---------------------------------------------------------------------------------------------------

    inner class MyBinder : Binder() {
        val service: GeoService
            get() = this@GeoService
    }
}
