package aman.zurutial.data

import android.content.Context

/**
 * All backend-facing behavior here (display name persistence) is unchanged from
 * the original implementation. Additions below are purely presentational
 * preferences for the redesigned Settings screen — appearance and developer
 * options — and don't affect sync/room/networking logic.
 */
object SettingsManager {
    private const val PREFS_NAME = "zurutial_settings"
    private const val KEY_DISPLAY_NAME = "display_name"

    private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    private const val KEY_DARK_THEME_MODE = "dark_theme_mode" // "system" | "light" | "dark"
    private const val KEY_PURE_BLACK = "pure_black"
    private const val KEY_AUTO_SYNC = "auto_sync"
    private const val KEY_SEEK_SENSITIVITY = "seek_sensitivity" // 0f..1f
    private const val KEY_DEBUG_LOGS_ENABLED = "debug_logs_enabled"

    fun getDisplayName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DISPLAY_NAME, "") ?: ""
    }

    fun setDisplayName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DISPLAY_NAME, name).apply()
    }

    fun getDynamicColorEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DYNAMIC_COLOR, true)

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    /** One of "system", "light", "dark". */
    fun getDarkThemeMode(context: Context): String =
        prefs(context).getString(KEY_DARK_THEME_MODE, "system") ?: "system"

    fun setDarkThemeMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_DARK_THEME_MODE, mode).apply()
    }

    fun getPureBlackEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PURE_BLACK, false)

    fun setPureBlackEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PURE_BLACK, enabled).apply()
    }

    fun getAutoSyncEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_SYNC, true)

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    fun getSeekSensitivity(context: Context): Float =
        prefs(context).getFloat(KEY_SEEK_SENSITIVITY, 0.5f)

    fun setSeekSensitivity(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_SEEK_SENSITIVITY, value).apply()
    }

    fun getDebugLogsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DEBUG_LOGS_ENABLED, false)

    fun setDebugLogsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DEBUG_LOGS_ENABLED, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
