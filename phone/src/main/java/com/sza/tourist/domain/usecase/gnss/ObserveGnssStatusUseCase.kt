package com.sza.tourist.domain.usecase.gnss

import com.sza.tourist.core.di.IoDispatcher
import com.sza.tourist.data.gnss.GnssStatusDataSource
import com.sza.tourist.domain.model.gnss.GnssSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * The GNSS status read path.
 *
 * The source already emits domain types, so this use case exists for the dispatcher alone - and that is not a
 * formality: `GnssStatusDataSource` applies no `flowOn`, and both `registerGnssStatusCallback` and
 * `requestLocationUpdates` are binder IPC that would otherwise run on the main thread the moment a screen
 * subscribed.
 *
 * It also keeps `ui` from importing `data`, which is what a screen would have to do to reach the source.
 */
class ObserveGnssStatusUseCase @Inject constructor(
    private val dataSource: GnssStatusDataSource,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(): Flow<GnssSnapshot> = dataSource.observe().flowOn(ioDispatcher)
}
