package cisneros.nota.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cisneros.nota.data.AppDb
import cisneros.nota.data.NoteEntity
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
    val createdAt: Long = System.currentTimeMillis()
)

// Estado de la pantalla principal
data class UiState(
    val items: List<NoteEntity> = emptyList(),   // lista completa
    val results: List<NoteEntity> = emptyList(), // resultados de búsqueda
    val query: String = "",
    val editing: EditingNote? = null             // null = lista, no hay editor abierto
)

class NoteVm(app: Application) : AndroidViewModel(app) {

    // ---- DB/DAO ----
    private val dao = AppDb.get(app).noteDao()

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
                    createdAt = System.currentTimeMillis()
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
                        createdAt = note.createdAt
                    )
                )
            }
        }
    }

    // =========================
    // ACTUALIZAR CAMPOS DEL EDITOR
    // =========================
    fun updateEditing(title: String? = null, body: String? = null) {
        val current = _state.value.editing ?: return
        val updated = current.copy(
            title = title ?: current.title,
            body = body ?: current.body
        )
        _state.update { it.copy(editing = updated) }
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
                    createdAt = e.createdAt
                )
            )
        } else {
            // Editar existente
            dao.update(
                NoteEntity(
                    id = e.id,
                    title = e.title,
                    body = e.body,
                    createdAt = e.createdAt
                )
            )
            e.id
        }
    }

    fun saveEditing() {
        val e = _state.value.editing ?: return

        // Si está completamente vacía, no guardamos nada
        if (e.title.isBlank() && e.body.isBlank()) {
            _state.update { it.copy(editing = null) }
            return
        }

        viewModelScope.launch {
            val id = upsertFromVm(e)
            _state.update { st ->
                st.copy(
                    editing = st.editing?.copy(id = id)
                )
            }
        }
    }

    /** Programa guardado automático tras [delayMs] ms sin más cambios. */
    fun autoSaveIfDirty(delayMs: Long = 800L) {
        autoSaveJob?.cancel()
        val e = _state.value.editing ?: return
        if (e.title.isBlank() && e.body.isBlank()) return

        autoSaveJob = viewModelScope.launch {
            delay(delayMs)
            saveEditing()
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

    fun closeEditor() {
        _state.update { it.copy(editing = null) }
    }
}
