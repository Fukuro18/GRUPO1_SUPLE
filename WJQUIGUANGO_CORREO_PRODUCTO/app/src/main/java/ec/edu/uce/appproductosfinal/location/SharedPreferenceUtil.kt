package ec.edu.uce.appproductosfinal.location

import android.content.Context
import androidx.core.content.edit

internal object SharedPreferenceUtil {
    private const val PREFS_NAME = "ec.edu.uce.appproductos_location_prefs"
    const val KEY_FOREGROUND_ENABLED = "tracking_foreground_location"
    private const val KEY_USER_NAME = "logged_user_name"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    // Nuevas claves para gestión de sesión
    private const val KEY_SESSION_START_TIME = "session_start_time"
    private const val KEY_LAST_ACTIVITY_TIME = "last_activity_time"

    fun getLocationTrackingPref(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FOREGROUND_ENABLED, false)

    fun saveLocationTrackingPref(context: Context, requestingLocationUpdates: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_FOREGROUND_ENABLED, requestingLocationUpdates)
        }

    fun saveUserSession(context: Context, userName: String) {
        val currentTime = System.currentTimeMillis()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_USER_NAME, userName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_SESSION_START_TIME, currentTime)
            putLong(KEY_LAST_ACTIVITY_TIME, currentTime)
        }
    }

    fun updateLastActivity(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(KEY_LAST_ACTIVITY_TIME, System.currentTimeMillis())
        }
    }

    fun getUserSession(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            prefs.getString(KEY_USER_NAME, null)
        } else null
    }

    fun isSessionValid(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return false

        val currentTime = System.currentTimeMillis()
        val startTime = prefs.getLong(KEY_SESSION_START_TIME, 0L)
        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY_TIME, 0L)

        val fifteenMinutes = 15 * 60 * 1000L
        val fiveMinutes = 5 * 60 * 1000L

        // Regla 1: Duración máxima 15 min
        if (currentTime - startTime > fifteenMinutes) return false
        
        // Regla 2: Inactividad máxima 5 min
        if (currentTime - lastActivity > fiveMinutes) return false

        return true
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_USER_NAME)
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_SESSION_START_TIME)
            remove(KEY_LAST_ACTIVITY_TIME)
        }
    }
}
