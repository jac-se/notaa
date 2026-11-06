package cisneros.nota.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cisneros.nota.data.AppDb
import cisneros.nota.data.NoteEntity
import cisneros.nota.data.ReminderEntity
import cisneros.nota.work.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Representa lo que está en el editor (nueva o existente)
data class EditingNote(
    val id: Long? = null,
    val title: String = "",
    val body: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val reminderAt: Long? = null
)

// Estado de la pantalla principal
data class UiState(
    val items: List<NoteEntity> = emptyList(),   // lista completa
    val results: List<NoteEntity> = emptyList(), // resultados de búsqueda
    val query: String = "",
    val reminders: List<ReminderEntity> = emptyList(),
    val editing: EditingNote? = null             // null = lista, no hay editor abierto
)

class NoteVm(app: Application) : AndroidViewModel(app) {

    // ---- DB/DAO ----
    private val dao = AppDb.get(app).noteDao()
    private val reminderDao = AppDb.get(app).reminderDao()

    // ---- Estado ----
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // ---- Autosave ----
    private var autoSaveJob: Job? = null
    private var searchJob: Job? = null

    init {
        // Nos suscribimos a la lista de notas activas
        viewModelScope.launch {
            dao.streamAll().collect { notes ->
                _state.update { it.copy(items = notes) }
            }
        }
        viewModelScope.launch {
            reminderDao.streamAll().collect { reminders ->
                _state.update { it.copy(reminders = reminders) }
            }
        }
    }

    // =========================
    // BÚSQUEDA
    // =========================
    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()

        if (q.isBlank()) {
            _state.update { it.copy(results = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            val res = dao.search(q)
            _state.update { it.copy(results = res) }
        }
    }

    // =========================
    // NUEVA NOTA
    // =========================
    fun newNote() {
        _state.update {
            it.copy(
                editing = EditingNote(
                    id = null,
                    title = "",
                    body = "",
                    createdAt = System.currentTimeMillis(),
                    reminderAt = null
                )
            )
        }
    }

    // =========================
    // EDITAR NOTA EXISTENTE
    // =========================
    fun edit(id: Long) {
        viewModelScope.launch {
            val note = dao.getById(id) ?: return@launch
            _state.update {
                it.copy(
                    editing = EditingNote(
                        id = note.id,
                        title = note.title,
                        body = note.body,
                        createdAt = note.createdAt,
                        reminderAt = note.reminderAt
                    )
                )
            }
        }
    }

    // =========================
    // ACTUALIZAR CAMPOS DEL EDITOR
    // =========================
    fun updateEditing(title: String? = null, body: String? = null, reminderAt: Long? = _state.value.editing?.reminderAt) {
        val current = _state.value.editing ?: return
        val updated = current.copy(
            title = title ?: current.title,
            body = body ?: current.body,
            reminderAt = reminderAt
        )
        _state.update { it.copy(editing = updated) }
        autoSaveIfDirty()
    }

    // =========================
    // GUARDAR (INSERT / UPDATE)
    // =========================
    private suspend fun upsertFromVm(e: EditingNote): Long {
        return if (e.id == null) {
            // Nueva
            dao.insert(
                NoteEntity(
                    title = e.title.ifBlank { "" },
                    body = e.body,
                    createdAt = e.createdAt,
                    reminderAt = e.reminderAt
                )
            )
        } else {
            // Editar existente
            dao.update(
                NoteEntity(
                    id = e.id,
                    title = e.title,
                    body = e.body,
                    createdAt = e.createdAt,
                    reminderAt = e.reminderAt
                )
            )
            e.id
        }
    }

    fun saveEditing() {
        persistEditing(_state.value.editing ?: return)
    }

    /** Programa guardado automático tras [delayMs] ms sin más cambios. */
    fun autoSaveIfDirty(delayMs: Long = 800L) {
        autoSaveJob?.cancel()
        val snapshot = _state.value.editing ?: return
        if (snapshot.title.isBlank() && snapshot.body.isBlank()) return

        autoSaveJob = viewModelScope.launch {
            delay(delayMs)
            persistSnapshot(snapshot)
        }
    }

    /** Guarda inmediatamente el contenido actual, por ejemplo al pausar la Activity. */
    fun flushAutoSave() {
        autoSaveJob?.cancel()
        val snapshot = _state.value.editing ?: return
        if (snapshot.title.isBlank() && snapshot.body.isBlank()) return
        autoSaveJob = viewModelScope.launch { persistSnapshot(snapshot) }
    }

    private fun persistEditing(snapshot: EditingNote) {
        autoSaveJob?.cancel()
        if (snapshot.title.isBlank() && snapshot.body.isBlank()) return
        autoSaveJob = viewModelScope.launch { persistSnapshot(snapshot) }
    }

    private suspend fun persistSnapshot(snapshot: EditingNote) {
        val id = upsertFromVm(snapshot)
        _state.update { current ->
            val editing = current.editing
            if (editing != null && editing.createdAt == snapshot.createdAt) {
                current.copy(editing = editing.copy(id = id))
            } else {
                current
            }
        }
    }

    // =========================
    // BORRAR (papelera o definitivo)
    // =========================
    fun deleteNote(id: Long, hardDelete: Boolean = false) {
        viewModelScope.launch {
            if (hardDelete) {
                dao.deleteById(id)              // borrado definitivo
            } else {
                dao.moveToTrash(id, System.currentTimeMillis()) // papelera
            }

            // Si justo la que se estaba editando era esa, cierra el editor
            _state.update { st ->
                if (st.editing?.id == id) st.copy(editing = null) else st
            }
        }
    }

    /** Recupera una copia sin sobrescribir las notas actuales. */
    fun importBackup(notes: List<NoteEntity>, reminders: List<ReminderEntity> = emptyList()) {
        viewModelScope.launch {
            notes.forEach { note ->
                if (!dao.exists(note.title, note.body, note.createdAt)) {
                    dao.insert(note.copy(id = 0))
                }
            }
            reminders.forEach { reminder ->
                if (!reminderDao.exists(reminder.category, reminder.description, reminder.triggerAt)) {
                    val id = reminderDao.insert(reminder.copy(id = 0))
                    ReminderScheduler.schedule(getApplication(), reminder.copy(id = id))
                }
            }
        }
    }

    fun createReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val id = reminderDao.insert(reminder)
            ReminderScheduler.schedule(getApplication(), reminder.copy(id = id))
        }
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderDao.update(reminder)
            ReminderScheduler.cancel(getApplication(), reminder.id)
            ReminderScheduler.schedule(getApplication(), reminder)
        }
    }

    fun completeReminder(id: Long) {
        viewModelScope.launch {
            reminderDao.setCompleted(id, true)
            ReminderScheduler.cancel(getApplication(), id)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), id)
            reminderDao.deleteById(id)
        }
    }

    fun closeEditor() {
        autoSaveJob?.cancel()
        val snapshot = _state.value.editing
        _state.update { it.copy(editing = null) }

        if (snapshot != null && (snapshot.title.isNotBlank() || snapshot.body.isNotBlank())) {
            autoSaveJob = viewModelScope.launch { persistSnapshot(snapshot) }
        }
    }
}
