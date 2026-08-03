package aman.zurutial.data

import android.content.Context

data class RecentRoom(
    val roomCode: String,
    val lastUsedAt: Long,
    val videoName: String
)

/**
 * Stores recently created/joined rooms for one-tap reconnect on the Rooms tab.
 * Local-only (SharedPreferences) — has no effect on the Firebase room/sync backend.
 */
object RecentRoomsManager {
    private const val PREFS_NAME = "recent_rooms_prefs"
    private const val KEY_ROOMS = "recent_rooms"
    private const val ENTRY_SEP = "\n"
    private const val FIELD_SEP = "\u0001"
    private const val MAX_ENTRIES = 20

    /** Kept for backward-compat call sites; records with an unknown video name. */
    fun addRoom(context: Context, roomCode: String, videoName: String = "") {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = readEntries(prefs).filterNot { it.roomCode == roomCode }.toMutableList()
        existing.add(0, RecentRoom(roomCode, System.currentTimeMillis(), videoName))
        writeEntries(prefs, existing.take(MAX_ENTRIES))
    }

    fun removeRoom(context: Context, roomCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = readEntries(prefs).filterNot { it.roomCode == roomCode }
        writeEntries(prefs, existing)
    }

    fun getRecentRooms(context: Context): List<String> =
        getRecentRoomDetails(context).map { it.roomCode }

    fun getRecentRoomDetails(context: Context): List<RecentRoom> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readEntries(prefs).sortedByDescending { it.lastUsedAt }
    }

    private fun readEntries(prefs: android.content.SharedPreferences): List<RecentRoom> {
        val raw = prefs.getString(KEY_ROOMS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(ENTRY_SEP).mapNotNull { line ->
            val parts = line.split(FIELD_SEP)
            if (parts.size < 2) return@mapNotNull null
            val code = parts[0]
            val ts = parts[1].toLongOrNull() ?: 0L
            val name = parts.getOrNull(2) ?: ""
            if (code.isBlank()) null else RecentRoom(code, ts, name)
        }
    }

    private fun writeEntries(prefs: android.content.SharedPreferences, entries: List<RecentRoom>) {
        val raw = entries.joinToString(ENTRY_SEP) { "${it.roomCode}$FIELD_SEP${it.lastUsedAt}$FIELD_SEP${it.videoName}" }
        prefs.edit().putString(KEY_ROOMS, raw).apply()
    }
}
