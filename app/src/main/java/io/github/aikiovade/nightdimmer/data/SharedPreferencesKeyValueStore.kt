package io.github.aikiovade.nightdimmer.data

import android.content.Context
import androidx.core.content.edit

/** [KeyValueStore] backed by [android.content.SharedPreferences]. */
class SharedPreferencesKeyValueStore(context: Context) : KeyValueStore {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)

    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    override fun putFloat(key: String, value: Float) {
        prefs.edit { putFloat(key, value) }
    }

    override fun putInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    private companion object {
        const val PREFS_NAME = "dimmer_prefs"
    }
}
