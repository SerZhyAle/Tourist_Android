package com.sza.tourist.data.gnss

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.sza.tourist.domain.model.gnss.GnssConstellation
import com.sza.tourist.domain.model.gnss.GnssCoordinate
import com.sza.tourist.domain.model.gnss.GnssSatellite
import com.sza.tourist.domain.model.gnss.GnssSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes the satellite constellation and the current position while a screen is open.
 *
 * Cold and foreground-only. The callback and the location request are made when a screen collects and are
 * both dropped when it stops, which is the whole of the "no background location" guarantee - there is no
 * service, no pending intent and no passive listener anywhere in this class, so nothing can outlive the
 * screen even if a caller forgets to cancel.
 *
 * Applies no `flowOn`: the dispatcher belongs to the consumer.
 */
@Singleton
class GnssStatusDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * Emits a snapshot per satellite or position update.
     *
     * A device that can show nothing - no location service, no fine-location grant, a missing or disabled
     * GPS provider - gets an empty flow: no permission and no waiting will produce a satellite there. A
     * working receiver that merely sees none emits an empty snapshot at once, so the reader tells the two
     * apart by whether any snapshot arrives, and never draws an empty sky the user would read as "no
     * satellites overhead" for a receiver that is not there.
     */
    fun observe(): Flow<GnssSnapshot> {
        val manager = locationManager
        return if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && canObserve(manager)) {
            observeStatus(manager)
        } else {
            emptyFlow()
        }
    }

    private fun canObserve(manager: LocationManager): Boolean = hasFineLocation() && isGpsUsable(manager)

    @RequiresApi(Build.VERSION_CODES.N)
    private fun observeStatus(manager: LocationManager): Flow<GnssSnapshot> = callbackFlow {
        val state = GnssState()
        val handler = Handler(Looper.getMainLooper())

        val statusCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                state.satellites = status.toSatellites()
                trySend(state.snapshot())
            }

            override fun onStopped() {
                state.satellites = emptyList()
                trySend(state.snapshot())
            }
        }
        val locationListener = positionListener { location ->
            state.coordinate = location.toCoordinate()
            trySend(state.snapshot())
        }

        val registered = register(manager, statusCallback, locationListener, handler)
        if (registered) {
            trySend(state.snapshot())
        }

        awaitClose {
            if (registered) {
                unregister(manager, statusCallback, locationListener)
            }
        }
    }

    /**
     * Registers both sources, or reports that the platform refused.
     *
     * Two different refusals, both handled. A `SecurityException` is possible even with the grant recorded -
     * the user can revoke it between the check and this call - and is caught rather than allowed to escape
     * the flow builder, which would skip `awaitClose` and leave whichever source did register attached for
     * the life of the process. The quieter one is `registerGnssStatusCallback` returning false: the receiver
     * declined without throwing, and treating that as success would emit the initial empty snapshot and show
     * an empty sky forever instead of nothing.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun register(
        manager: LocationManager,
        statusCallback: GnssStatus.Callback,
        locationListener: LocationListener,
        handler: Handler,
    ): Boolean = try {
        if (manager.registerGnssStatusCallback(statusCallback, handler)) {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_UPDATE_INTERVAL_MS,
                MIN_UPDATE_DISTANCE_METERS,
                locationListener,
                Looper.getMainLooper(),
            )
            true
        } else {
            Timber.w("GNSS status callback declined by the receiver")
            false
        }
    } catch (security: SecurityException) {
        Timber.w(security, "GNSS observation refused despite a granted permission")
        manager.unregisterGnssStatusCallback(statusCallback)
        false
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun unregister(
        manager: LocationManager,
        statusCallback: GnssStatus.Callback,
        locationListener: LocationListener,
    ) {
        manager.unregisterGnssStatusCallback(statusCallback)
        manager.removeUpdates(locationListener)
    }

    /**
     * The position sink.
     *
     * Every member is overridden rather than relying on the interface defaults, which only exist from API 30:
     * a listener that leaves them out is called into on an older platform and dies with `AbstractMethodError`.
     */
    private fun positionListener(onLocation: (Location) -> Unit): LocationListener =
        object : LocationListener {
            override fun onLocationChanged(location: Location) = onLocation(location)

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

    private fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun isGpsUsable(manager: LocationManager): Boolean =
        LocationManager.GPS_PROVIDER in manager.allProviders &&
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    /** Mutable view of the two sources, mutated only on the main looper both callbacks are posted to. */
    private class GnssState {

        var satellites: List<GnssSatellite> = emptyList()
        var coordinate: GnssCoordinate? = null

        fun snapshot(): GnssSnapshot = GnssSnapshot(
            satellites = satellites,
            coordinate = coordinate,
            takenAtMillis = System.currentTimeMillis(),
        )
    }

    private companion object {

        /** Position updates are for a readout, not navigation - once a second is plenty. */
        const val MIN_UPDATE_INTERVAL_MS = 1000L

        /** Every update is wanted, including a stationary one: a fresh fix time is itself a signal. */
        const val MIN_UPDATE_DISTANCE_METERS = 0f
    }
}

/** The platform reports an undecoded satellite as this, not as an absent measurement. */
private const val CN0_UNREPORTED = 0f

@RequiresApi(Build.VERSION_CODES.N)
private fun GnssStatus.toSatellites(): List<GnssSatellite> = (0 until satelliteCount).map { index ->
    GnssSatellite(
        constellation = constellationOf(getConstellationType(index)),
        satelliteId = getSvid(index),
        cn0DbHz = getCn0DbHz(index).takeIf { it > CN0_UNREPORTED }?.toDouble(),
        usedInFix = usedInFix(index),
        elevationDegrees = getElevationDegrees(index),
        azimuthDegrees = getAzimuthDegrees(index),
    )
}

@RequiresApi(Build.VERSION_CODES.N)
private fun constellationOf(type: Int): GnssConstellation = when (type) {
    GnssStatus.CONSTELLATION_GPS -> GnssConstellation.GPS
    GnssStatus.CONSTELLATION_GLONASS -> GnssConstellation.GLONASS
    GnssStatus.CONSTELLATION_GALILEO -> GnssConstellation.GALILEO
    GnssStatus.CONSTELLATION_BEIDOU -> GnssConstellation.BEIDOU
    GnssStatus.CONSTELLATION_QZSS -> GnssConstellation.QZSS
    GnssStatus.CONSTELLATION_SBAS -> GnssConstellation.SBAS
    else -> irnssOrUnknown(type)
}

/** IRNSS/NavIC only became a named constellation in API 29; below that it arrives as an unknown type. */
private fun irnssOrUnknown(type: Int): GnssConstellation =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && type == GnssStatus.CONSTELLATION_IRNSS) {
        GnssConstellation.IRNSS
    } else {
        GnssConstellation.UNKNOWN
    }

private fun Location.toCoordinate(): GnssCoordinate = GnssCoordinate(
    latitudeDegrees = latitude,
    longitudeDegrees = longitude,
    accuracyMeters = if (hasAccuracy()) accuracy else null,
    fixedAtMillis = time,
)
