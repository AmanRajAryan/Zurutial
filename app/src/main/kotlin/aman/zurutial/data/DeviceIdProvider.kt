package aman.zurutial.data

import android.content.Context
import java.util.UUID

object DeviceIdProvider {
    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }
}
