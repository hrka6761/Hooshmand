package ir.hrka.datastore.api

import kotlinx.coroutines.flow.Flow

/**
 * Typed key-value storage for primitive values backed by Preferences DataStore.
 *
 * Use [Flow]-based readers to observe changes and suspend writers to persist updates.
 * Keys are namespaced internally per type, so the same [key] string can be used for
 * different primitive types without collision.
 *
 * Inject this interface from Hilt in the host application module.
 *
 * @see ir.hrka.datastore.PrimitivePreferencesDataSource
 */
interface PrimitiveDataStore {

    /** Observes an [Int] value for [key]. */
    fun intFlow(key: String, default: Int = 0): Flow<Int>

    /** Observes a [Long] value for [key]. */
    fun longFlow(key: String, default: Long = 0L): Flow<Long>

    /** Observes a [Float] value for [key]. */
    fun floatFlow(key: String, default: Float = 0f): Flow<Float>

    /** Observes a [Double] value for [key]. */
    fun doubleFlow(key: String, default: Double = 0.0): Flow<Double>

    /** Observes a [Boolean] value for [key]. */
    fun booleanFlow(key: String, default: Boolean = false): Flow<Boolean>

    /** Observes a [String] value for [key]. */
    fun stringFlow(key: String, default: String = ""): Flow<String>

    /** Reads the current [Int] value for [key]. */
    suspend fun getInt(key: String, default: Int = 0): Int

    /** Reads the current [Long] value for [key]. */
    suspend fun getLong(key: String, default: Long = 0L): Long

    /** Reads the current [Float] value for [key]. */
    suspend fun getFloat(key: String, default: Float = 0f): Float

    /** Reads the current [Double] value for [key]. */
    suspend fun getDouble(key: String, default: Double = 0.0): Double

    /** Reads the current [Boolean] value for [key]. */
    suspend fun getBoolean(key: String, default: Boolean = false): Boolean

    /** Reads the current [String] value for [key]. */
    suspend fun getString(key: String, default: String = ""): String

    /** Persists an [Int] value for [key]. */
    suspend fun putInt(key: String, value: Int)

    /** Persists a [Long] value for [key]. */
    suspend fun putLong(key: String, value: Long)

    /** Persists a [Float] value for [key]. */
    suspend fun putFloat(key: String, value: Float)

    /** Persists a [Double] value for [key]. */
    suspend fun putDouble(key: String, value: Double)

    /** Persists a [Boolean] value for [key]. */
    suspend fun putBoolean(key: String, value: Boolean)

    /** Persists a [String] value for [key]. */
    suspend fun putString(key: String, value: String)

    /** Removes all primitive values stored under [key]. */
    suspend fun remove(key: String)

    /** Clears every value in the underlying preferences file. */
    suspend fun clear()
}
