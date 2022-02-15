package ns.fajnet.android.puftocatorclient.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ns.fajnet.android.puftocatorclient.common.Constants
import ns.fajnet.android.puftocatorclient.common.LogEx
import ns.fajnet.android.puftocatorclient.services.GeoService

object ServiceRepository {

    // members -------------------------------------------------------------------------------------

    private var geoReceiverServiceReferenceMutable = MutableLiveData<GeoService>()

    // properties ----------------------------------------------------------------------------------

    val geoReceiverServiceReference: LiveData<GeoService>
        get() = geoReceiverServiceReferenceMutable

    // public methods ------------------------------------------------------------------------------

    fun setGeoReceiverServiceReference(geoTrackServiceReference: GeoService) {
        LogEx.d(Constants.TAG_SERVICE_REPOSITORY, "setting GeoTrackServiceReference")
        geoReceiverServiceReferenceMutable.postValue(geoTrackServiceReference)
    }

    fun unsetGeoReceiverServiceReference() {
        LogEx.d(Constants.TAG_SERVICE_REPOSITORY, "unsetting GeoTrackServiceReference")
        geoReceiverServiceReferenceMutable.postValue(null)
    }
}
