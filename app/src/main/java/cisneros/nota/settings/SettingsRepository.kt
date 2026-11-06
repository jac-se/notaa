package cisneros.nota.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore("settings")

object Keys {
    val BODY_SIZE = intPreferencesKey("body_size_level")
    val MEDICINE_REMINDER_AT = longPreferencesKey("medicine_reminder_at")
}

class SettingsRepository(private val context: Context) {
    val bodySizeLevel: Flow<Int> = context.dataStore.data.map { it[Keys.BODY_SIZE] ?: 1 }
    val medicineReminderAt: Flow<Long?> = context.dataStore.data.map { it[Keys.MEDICINE_REMINDER_AT] }

    suspend fun setBodySizeLevel(level: Int) {
        context.dataStore.edit { it[Keys.BODY_SIZE] = level.coerceIn(0, 4) }
    }

    suspend fun setMedicineReminderAt(millis: Long?) {
        context.dataStore.edit {
            if (millis == null) it.remove(Keys.MEDICINE_REMINDER_AT)
            else it[Keys.MEDICINE_REMINDER_AT] = millis
        }
    }
}
