package aman.zurutial.data

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "zurutial_settings"
    private const val KEY_DISPLAY_NAME = "display_name"

    fun getDisplayName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DISPLAY_NAME, "") ?: ""
    }

    fun setDisplayName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DISPLAY_NAME, name).apply()
    }
}
