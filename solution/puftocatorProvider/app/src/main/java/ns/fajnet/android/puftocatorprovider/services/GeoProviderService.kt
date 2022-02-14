package ns.fajnet.android.puftocatorprovider.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import ns.fajnet.android.puftocatorprovider.common.Constants
import ns.fajnet.android.puftocatorprovider.common.LogEx

class GeoProviderService : Service() {

    override fun onCreate() {
        super.onCreate()
        LogEx.d(Constants.TAG_GEO_PROVIDER_SERVICE, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogEx.d(Constants.TAG_GEO_PROVIDER_SERVICE, "onStartCommand")

        // TODO: start service only if it is not started yet

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
