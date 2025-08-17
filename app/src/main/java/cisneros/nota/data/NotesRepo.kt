// app/src/main/java/cisneros/nota/data/NotesRepo.kt
// NUEVO: repositorio para encapsular NoteDao y exponer funciones claras.

package cisneros.nota.data

import kotlinx.coroutines.flow.Flow

class NotesRepo(private val dao: NoteDao) {

    fun observeActive(query: String): Flow<List<NoteEntity>> = dao.observeAll(query)

    fun observeTrash(query: String): Flow<List<NoteEntity>> = dao.observeTrash(query)

    suspend fun upsert(note: NoteEntity): Long {
        return if (note.id == 0L) dao.insert(note) else {
            dao.update(note); note.id
        }
    }

    suspend fun get(id: Long): NoteEntity? = dao.getById(id)

    suspend fun moveToTrash(id: Long, atMillis: Long) = dao.moveToTrash(id, atMillis)

    suspend fun restore(id: Long) = dao.restore(id)

    suspend fun deleteForever(id: Long) = dao.deleteById(id)

    suspend fun emptyTrash() = dao.emptyTrash()
}
