package aman.zurutial.sync

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.awaitAll
import java.util.UUID

data class OffsetSample(val offsetMs: Long, val roundTripMs: Long)

class ClockSync(
    private val db: DatabaseReference,
    private val onLog: (String) -> Unit = {}
) {

    private var currentOffsetMs: Long = 0L

    /**
     * Runs N samples, keeps the best half (lowest round-trip), returns median offset.
     * offsetMs represents: serverTime = localTime + offsetMs
     */
    suspend fun estimateOffset(sampleCount: Int = 8): Long = coroutineScope {
        try {
            onLog("Starting clock sync ($sampleCount pings)...")
            val deferredSamples = (1..sampleCount).map { i ->
                async {
                    delay(i * 50L) // Stagger the requests slightly
                    takeSingleSample()
                }
            }
            val samples = deferredSamples.awaitAll()

            val bestSamples = samples.sortedBy { it.roundTripMs }.take(sampleCount / 2)
            val medianOffset = if (bestSamples.isNotEmpty()) bestSamples.map { it.offsetMs }.sorted()[bestSamples.size / 2] else 0L

            onLog("Clock Sync complete. (Best ${bestSamples.size} used)")
            samples.forEachIndexed { index, sample ->
                val isUsed = bestSamples.contains(sample)
                val status = if (isUsed) "[USED]" else "[DISCARDED]"
                onLog("  -> Ping ${index + 1}: round-trip=${sample.roundTripMs}ms, offset=${sample.offsetMs}ms $status")
            }

            currentOffsetMs = medianOffset
            return@coroutineScope medianOffset
        } catch (e: Exception) {
            onLog("Clock sync failed: ${e.message}")
            currentOffsetMs = 0L
            return@coroutineScope 0L
        }
    }

    private suspend fun takeSingleSample(): OffsetSample = suspendCancellableCoroutine { cont ->
        val t0 = System.currentTimeMillis()
        val probeRef = db.child("_clockProbe").child(UUID.randomUUID().toString())

        probeRef.setValue(ServerValue.TIMESTAMP)
            .addOnSuccessListener {
                probeRef.get().addOnSuccessListener { snapshot ->
                    val t1 = System.currentTimeMillis()
                    val serverTime = snapshot.value as Long
                    val roundTrip = t1 - t0
                    val estimatedLocalTimeAtServerStamp = (t0 + t1) / 2
                    val offset = serverTime - estimatedLocalTimeAtServerStamp

                    probeRef.removeValue() // cleanup, don't leave probe litter in the DB
                    cont.resume(OffsetSample(offset, roundTrip), onCancellation = null)
                }.addOnFailureListener { cont.cancel(it) }
            }
            .addOnFailureListener { cont.cancel(it) }
    }

    /** Convert a local timestamp to estimated server time */
    fun toServerTime(localTimeMs: Long): Long = localTimeMs + currentOffsetMs

    /** Convert a server timestamp to estimated local time */
    fun toLocalTime(serverTimeMs: Long): Long = serverTimeMs - currentOffsetMs

    fun getOffset(): Long = currentOffsetMs
}
