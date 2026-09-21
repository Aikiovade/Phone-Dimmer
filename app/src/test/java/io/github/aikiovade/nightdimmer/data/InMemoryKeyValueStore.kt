package io.github.aikiovade.nightdimmer.data

/**
 * In-memory [KeyValueStore] used by unit tests.
 *
 * Values are stored untyped so a test can also seed deliberately corrupted data.
 */
class InMemoryKeyValueStore(values: Map<String, Any> = emptyMap()) : KeyValueStore {

    private val values = values.toMutableMap()

    override fun getBoolean(key: String, default: Boolean): Boolean =
        values[key] as? Boolean ?: default

    override fun getFloat(key: String, default: Float): Float = values[key] as? Float ?: default

    override fun getInt(key: String, default: Int): Int = values[key] as? Int ?: default

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun putFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }
}
