// app/src/main/java/cisneros/nota/data/AppDb.kt
// CAMBIO: base limpia con version = 1, sin migraciones fantasmas duplicadas.

package cisneros.nota.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var INSTANCE: AppDb? = null

        fun get(context: Context): AppDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "notes.db"
                ).build().also { INSTANCE = it }
            }
    }
}
