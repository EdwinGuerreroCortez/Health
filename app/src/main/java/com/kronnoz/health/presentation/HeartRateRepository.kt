package com.kronnoz.health.presentation

import android.content.Context
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

class HeartRateRepository(context: Context) {
    private val client = HealthServices.getClient(context).measureClient

    suspend fun isAvailable(): Boolean {
        return DataType.HEART_RATE_BPM in client.getCapabilitiesAsync().await().supportedDataTypesMeasure
    }

    fun flow() = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                if (availability is DataTypeAvailability)
                    trySendBlocking(HeartRateResult.Available(availability == DataTypeAvailability.AVAILABLE))
            }

            override fun onDataReceived(data: DataPointContainer) {
                val bpm = data.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value ?: 0.0
                trySendBlocking(HeartRateResult.Data(bpm))
            }
        }

        client.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
        awaitClose { client.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback) }
    }
}

sealed class HeartRateResult {
    data class Data(val bpm: Double) : HeartRateResult()
    data class Available(val isAvailable: Boolean) : HeartRateResult()
}
