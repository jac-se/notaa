package cisneros.nota.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.datastore by preferencesDataStore("nota_prefs")
private val KEY_FONT_SP = intPreferencesKey("font_sp")

object FontPrefs {
    fun flow(context: Context) = context.datastore.data.map { p ->
        p[KEY_FONT_SP] ?: 24
    }
    suspend fun set(context: Context, sp: Int) {
        context.datastore.edit { it[KEY_FONT_SP] = sp }
    }
}
