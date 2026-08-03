package aman.zurutial.sync

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

class PlaybackController(
    private val roomRef: DatabaseReference,
    private val clockSync: ClockSync,
    private val deviceId: String,
    private val onLog: (String) -> Unit = {}
) {
    fun sendAction(positionMs: Long, isPlaying: Boolean, playbackSpeed: Float, actionType: String, actionId: String) {
        onLog("Tx [$actionType] play=$isPlaying pos=$positionMs speed=$playbackSpeed")
        val stateRef = roomRef.child("playbackState")

        val hostMeasuredAt = clockSync.toServerTime(System.currentTimeMillis())
        val updates = mapOf(
            "positionMs" to positionMs,
            "isPlaying" to isPlaying,
            "playbackSpeed" to playbackSpeed,
            "hostMeasuredAtServerTime" to hostMeasuredAt,
            "seekVersion" to hostMeasuredAt,
            "lastActionBy" to deviceId,
            "lastActionType" to actionType,
            "lastActionId" to actionId
        )
        
        stateRef.setValue(updates)
    }
}
