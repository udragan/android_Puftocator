package ns.fajnet.android.puftocatorclient

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import ns.fajnet.android.puftocatorclient.services.GeoService

class MapsActivityViewModel(application: Application) : AndroidViewModel(application) {

    // members ---------------------------------------------------------------------------------------------------------

    private val context: Context = application

    // public methods --------------------------------------------------------------------------------------------------

    fun startService() {
        val intent = Intent(context, GeoService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }
}
