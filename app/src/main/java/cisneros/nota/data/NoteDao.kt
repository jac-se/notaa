// app/src/main/java/cisneros/nota/data/NoteDao.kt
// DAO simple y claro: lista, búsqueda, CRUD y papelera.

package cisneros.nota.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // ===== LISTA PRINCIPAL (notas activas) =====
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun streamAll(): Flow<List<NoteEntity>>

    // ===== BÚSQUEDA (solo notas activas) =====
    @Query("""
        SELECT * FROM notes
        WHERE deletedAt IS NULL
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    suspend fun search(query: String): List<NoteEntity>

    // ===== CRUD =====
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ===== PAPELERA =====
    @Query("UPDATE notes SET deletedAt = :millis WHERE id = :id")
    suspend fun moveToTrash(id: Long, millis: Long)

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM notes WHERE deletedAt < :threshold")
    suspend fun deleteTrashOlderThan(threshold: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()
}
