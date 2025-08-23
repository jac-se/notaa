// app/src/main/java/cisneros/nota/data/NoteDao.kt
// CORRECCIÓN: una sola interfaz @Dao. Campos usados: id, title, content, createdAt, deletedAt.

package cisneros.nota.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {


    // ---- Streams principales ----
    @Query("""
        SELECT * FROM notes
        WHERE deletedAt IS NULL
        ORDER BY createdAt DESC
    """)
    fun streamAll(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE deletedAt IS NULL
          AND (title LIKE :q OR content LIKE :q)
        ORDER BY createdAt DESC
    """)
    suspend fun search(q: String): List<NoteEntity>

    // ---- Observables con filtro opcional ----
    @Query("""
        SELECT * FROM notes
        WHERE deletedAt IS NULL
          AND (:q == '' OR title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%')
        ORDER BY createdAt DESC
    """)
    fun observeAll(q: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE deletedAt IS NOT NULL
          AND (:q == '' OR title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%')
        ORDER BY createdAt DESC
    """)
    fun observeTrash(q: String): Flow<List<NoteEntity>>

    // ---- CRUD ----
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    // ---- Papelera ----
    @Query("UPDATE notes SET deletedAt = :millis WHERE id = :id")
    suspend fun moveToTrash(id: Long, millis: Long)

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM notes WHERE deletedAt < :threshold")
    suspend fun deleteTrashOlderThan(threshold: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()
}
