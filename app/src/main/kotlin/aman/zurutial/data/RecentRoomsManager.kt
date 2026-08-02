package aman.zurutial.data

import android.content.Context

object RecentRoomsManager {
    private const val PREFS_NAME = "recent_rooms_prefs"
    private const val KEY_ROOMS = "recent_rooms"

    fun addRoom(context: Context, roomCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentRooms = prefs.getStringSet(KEY_ROOMS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentRooms.add(roomCode)
        prefs.edit().putStringSet(KEY_ROOMS, currentRooms).apply()
    }

    fun removeRoom(context: Context, roomCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentRooms = prefs.getStringSet(KEY_ROOMS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentRooms.remove(roomCode)
        prefs.edit().putStringSet(KEY_ROOMS, currentRooms).apply()
    }

    fun getRecentRooms(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ROOMS, emptySet())?.toList() ?: emptyList()
    }
}
