package cisneros.nota.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore("settings")

object Keys { val BODY_SIZE = intPreferencesKey("body_size_level") } // 0=Normal,1=Grande,2=XL

class SettingsRepository(private val context: Context) {
    val bodySizeLevel: Flow<Int> = context.dataStore.data.map { it[Keys.BODY_SIZE] ?: 1 }
    suspend fun setBodySizeLevel(level: Int) {
        context.dataStore.edit { it[Keys.BODY_SIZE] = level.coerceIn(0,2) }
    }
}
