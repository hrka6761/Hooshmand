package ir.hrka.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ir.hrka.datastore.api.PrimitiveDataStore
import ir.hrka.datastore.di.HrkaPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Preferences DataStore-backed implementation of [PrimitiveDataStore].
 *
 * Raw `DataStore<Preferences>` is internal to the module; inject [PrimitiveDataStore] instead.
 */
internal class PrimitivePreferencesDataSource @Inject constructor(
    @param:HrkaPreferencesDataStore private val preferencesDataStore: DataStore<Preferences>,
) : PrimitiveDataStore {

    override fun intFlow(key: String, default: Int): Flow<Int> =
        preferencesDataStore.data.map { preferences ->
            preferences[intPreferencesKey(key)] ?: default
        }

    override fun longFlow(key: String, default: Long): Flow<Long> =
        preferencesDataStore.data.map { preferences ->
            preferences[longPreferencesKey(key)] ?: default
        }

    override fun floatFlow(key: String, default: Float): Flow<Float> =
        preferencesDataStore.data.map { preferences ->
            preferences[floatPreferencesKey(key)] ?: default
        }

    override fun doubleFlow(key: String, default: Double): Flow<Double> =
        preferencesDataStore.data.map { preferences ->
            preferences[doublePreferencesKey(key)]?.let(Double::fromBits) ?: default
        }

    override fun booleanFlow(key: String, default: Boolean): Flow<Boolean> =
        preferencesDataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(key)] ?: default
        }

    override fun stringFlow(key: String, default: String): Flow<String> =
        preferencesDataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: default
        }

    override suspend fun getInt(key: String, default: Int): Int =
        intFlow(key, default).first()

    override suspend fun getLong(key: String, default: Long): Long =
        longFlow(key, default).first()

    override suspend fun getFloat(key: String, default: Float): Float =
        floatFlow(key, default).first()

    override suspend fun getDouble(key: String, default: Double): Double =
        doubleFlow(key, default).first()

    override suspend fun getBoolean(key: String, default: Boolean): Boolean =
        booleanFlow(key, default).first()

    override suspend fun getString(key: String, default: String): String =
        stringFlow(key, default).first()

    override suspend fun putInt(key: String, value: Int) {
        preferencesDataStore.edit { preferences ->
            preferences[intPreferencesKey(key)] = value
        }
    }

    override suspend fun putLong(key: String, value: Long) {
        preferencesDataStore.edit { preferences ->
            preferences[longPreferencesKey(key)] = value
        }
    }

    override suspend fun putFloat(key: String, value: Float) {
        preferencesDataStore.edit { preferences ->
            preferences[floatPreferencesKey(key)] = value
        }
    }

    override suspend fun putDouble(key: String, value: Double) {
        preferencesDataStore.edit { preferences ->
            preferences[doublePreferencesKey(key)] = value.toRawBits()
        }
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }

    override suspend fun putString(key: String, value: String) {
        preferencesDataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun remove(key: String) {
        preferencesDataStore.edit { preferences ->
            preferences.remove(intPreferencesKey(key))
            preferences.remove(longPreferencesKey(key))
            preferences.remove(floatPreferencesKey(key))
            preferences.remove(doublePreferencesKey(key))
            preferences.remove(booleanPreferencesKey(key))
            preferences.remove(stringPreferencesKey(key))
        }
    }

    override suspend fun clear() {
        preferencesDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun doublePreferencesKey(key: String) = longPreferencesKey("double:$key")
}
