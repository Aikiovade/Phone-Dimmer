package io.github.aikiovade.nightdimmer.data

/**
 * Minimal persistence surface used by [SettingsRepository].
 *
 * Having this one level of indirection keeps the repository free of Android
 * types, which is what makes it testable on the JVM with an in-memory fake.
 */
interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getFloat(key: String, default: Float): Float
    fun getInt(key: String, default: Int): Int

    fun putBoolean(key: String, value: Boolean)
    fun putFloat(key: String, value: Float)
    fun putInt(key: String, value: Int)
}
