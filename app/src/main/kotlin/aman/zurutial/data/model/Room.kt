package aman.zurutial.data.model

data class Room(
    val roomCode: String = "",
    val roomCreatorId: String = "",
    val fileHash: String = "",       // fingerprint: "{sizeBytes}_{durationMs}"
    val fileName: String = "",
    val canMembersControlPlayback: Boolean = true,
    val createdAt: Long = 0L
)

data class PlaybackState(
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val hostMeasuredAtServerTime: Long = 0L,
    val seekVersion: Long = 0L,
    val lastActionBy: String = "",
    val lastActionType: String = "",
    val lastActionId: String = ""
)

data class Member(
    val deviceId: String = "",
    val displayName: String = "",
    val joinedAt: Long = 0L,
    val lastSeen: Long = 0L
)
