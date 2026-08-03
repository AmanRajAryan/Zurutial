package aman.zurutial.sync

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PingEngine(
    private val roomRef: DatabaseReference,
    private val deviceId: String
) {
    private val _serverPing = MutableStateFlow(0L)
    val serverPing: StateFlow<Long> = _serverPing.asStateFlow()

    private val _e2ePing = MutableStateFlow<Map<String, Long>>(emptyMap())
    val e2ePing: StateFlow<Map<String, Long>> = _e2ePing.asStateFlow()

    private val pingsRef = roomRef.child("pings")

    private val lastProcessedRequests = mutableMapOf<String, Long>()

    private val requestListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            snapshot.children.forEach { child ->
                val senderId = child.key ?: return@forEach
                val timestamp = child.getValue(Long::class.java) ?: return@forEach
                
                if (senderId != deviceId && timestamp > 0L) {
                    val lastProcessed = lastProcessedRequests[senderId] ?: 0L
                    if (timestamp > lastProcessed) {
                        lastProcessedRequests[senderId] = timestamp
                        pingsRef.child("responses").child(senderId).child(deviceId).setValue(timestamp)
                    }
                }
            }
        }
        override fun onCancelled(error: DatabaseError) {}
    }

    private val responseListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val currentMap = _e2ePing.value.toMutableMap()
            snapshot.children.forEach { child ->
                val responderId = child.key ?: return@forEach
                val originalTimestamp = child.getValue(Long::class.java) ?: return@forEach
                if (originalTimestamp > 0L) {
                    val rtt = System.currentTimeMillis() - originalTimestamp
                    if (rtt in 0..10_000) {
                        currentMap[responderId] = rtt
                    }
                }
            }
            _e2ePing.value = currentMap
        }
        override fun onCancelled(error: DatabaseError) {}
    }

    fun start() {
        pingsRef.child("requests").addValueEventListener(requestListener)
        pingsRef.child("responses").child(deviceId).addValueEventListener(responseListener)
    }

    fun stop() {
        pingsRef.child("requests").removeEventListener(requestListener)
        pingsRef.child("responses").child(deviceId).removeEventListener(responseListener)
    }

    fun measureServerPing() {
        val start = System.currentTimeMillis()
        pingsRef.child("serverPing").child(deviceId).setValue(ServerValue.TIMESTAMP).addOnCompleteListener {
            val end = System.currentTimeMillis()
            _serverPing.value = end - start
        }
    }

    fun measureE2EPing() {
        pingsRef.child("requests").child(deviceId).setValue(System.currentTimeMillis())
    }
}
