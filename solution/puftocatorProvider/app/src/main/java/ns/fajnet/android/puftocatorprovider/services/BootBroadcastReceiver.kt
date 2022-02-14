package ns.fajnet.android.puftocatorprovider.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ns.fajnet.android.puftocatorprovider.MainActivity

class BootBroadcastReceiver : BroadcastReceiver() {

    // overrides -------------------------------------------------------------------------------------------------------

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action.equals(Intent.ACTION_BOOT_COMPLETED, true)) {

            val activityIntent = Intent(context, MainActivity::class.java)
            context!!.startActivity(activityIntent)

            val serviceIntent = Intent(context, GeoProviderService::class.java)
            context!!.startService(serviceIntent)
        }
    }
}
