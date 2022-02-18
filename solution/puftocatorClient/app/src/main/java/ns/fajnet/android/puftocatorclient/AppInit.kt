package ns.fajnet.android.puftocatorclient

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import ns.fajnet.android.puftocatorclient.common.Constants

/**
 * Initialize global data/service channels
 */
class AppInit : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID_GEO_SERVICE,
                Constants.NOTIFICATION_CHANNEL_NAME_GEO_SERVICE,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.enableVibration(true)

            val notificationManager = getSystemService(
                NotificationManager::class.java
            )

            notificationManager.createNotificationChannel(serviceChannel)
        }
    }
}
