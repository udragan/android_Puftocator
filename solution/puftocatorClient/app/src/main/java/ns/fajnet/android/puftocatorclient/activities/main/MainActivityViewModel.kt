package ns.fajnet.android.puftocatorclient.activities.main

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx
import ns.fajnet.android.puftocatorclient.models.LocationInfo
import ns.fajnet.android.puftocatorclient.services.GeoService

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    // members ---------------------------------------------------------------------------------------------------------

    private val context: Context by lazy { application }
    private val _hasInternetConnection: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    private val _liveTargetLocation: MutableLiveData<LocationInfo?> by lazy { MutableLiveData<LocationInfo?>() }
    private val internetObserver: Observer<Boolean> = Observer { _hasInternetConnection.postValue(it) }
    private val targetObserver: Observer<LocationInfo?> = Observer { _liveTargetLocation.postValue(it) }

    private lateinit var geoService: GeoService

    // properties ------------------------------------------------------------------------------------------------------

    val hasInternetConnection: LiveData<Boolean>
        get() = _hasInternetConnection

    val liveTargetLocation: LiveData<LocationInfo?>
        get() = _liveTargetLocation

    // overrides  ------------------------------------------------------------------------------------------------------

    override fun onCleared() {
        super.onCleared()

        geoService.hasInternetConnection.removeObserver(internetObserver)
        geoService.liveTargetLocation.removeObserver(targetObserver)
        LogEx.d(Constants.TAG_MAIN_ACTIVITY_VIEW_MODEL, "clearing observer")
    }

    // public methods --------------------------------------------------------------------------------------------------

    fun startService() {
        LogEx.d(Constants.TAG_MAIN_ACTIVITY_VIEW_MODEL, "start service")
        val intent = Intent(context, GeoService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun setGeoService(service: GeoService) {
        LogEx.d(Constants.TAG_MAIN_ACTIVITY_VIEW_MODEL, "set geo service and observer")
        if (this::geoService.isInitialized) {
            LogEx.d(Constants.TAG_MAIN_ACTIVITY_VIEW_MODEL, "geo service already set, skipping..")
            return
        }

        geoService = service
        geoService.hasInternetConnection.observeForever(internetObserver)
        geoService.liveTargetLocation.observeForever(targetObserver)
    }
}
