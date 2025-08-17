package cisneros.nota.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import cisneros.nota.data.NoteEntity

/**
 * UI State para la pantalla de notas.
 * - items: lista de notas existentes
 * - editing: nota en edición (o null si no hay edición activa)
 */
data class UiState(
    val items: List<NoteEntity> = emptyList(),
    val editing: NoteEntity? = null
)

class NoteVm : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    // Generador local de IDs (cuando no hay BD). Si usas Room con AUTOINCREMENT, puedes ignorarlo.
    private var idCounter: Long = 0L

    /** Crea una nueva nota en memoria (sin guardar todavía) y la abre en el editor. */
    fun newNote() {
        _state.update { s ->
            s.copy(editing = NoteEntity(id = 0L, title = "", body = ""))
        }
    }

    /** Abre en edición la nota con el id indicado (si existe). */
    fun edit(id: Long) {
        _state.update { s ->
            val note = s.items.firstOrNull { it.id == id }
            s.copy(editing = note?.copy())
        }
    }

    /**
     * Actualiza los campos de la nota en edición.
     * Pasa solo los campos que quieras modificar (title/body).
     */
    fun updateEditing(title: String? = null, body: String? = null) {
        _state.update { s ->
            val e = s.editing ?: return@update s
            s.copy(
                editing = e.copy(
                    title = title ?: e.title,
                    body = body ?: e.body
                )
            )
        }
    }

    /**
     * Guarda la nota en edición.
     * - Si id == 0L -> inserta como nueva con id generado.
     * - Si id > 0L  -> actualiza la existente.
     * Mantiene la nota como editing (útil cuando el usuario sigue escribiendo).
     */
    fun saveEditing() {
        viewModelScope.launch {
            _state.update { s ->
                val e = s.editing ?: return@update s
                if (e.id == 0L) {
                    val newId = (++idCounter).coerceAtLeast(1L)
                    val saved = e.copy(id = newId)
                    s.copy(
                        items = s.items + saved,
                        editing = saved
                    )
                } else {
                    val updatedList = s.items.map { if (it.id == e.id) e else it }
                    s.copy(items = updatedList, editing = e)
                }
            }
        }
    }

    /**
     * Guarda automáticamente si hay una nota en edición.
     * Aquí asumimos que siempre conviene persistir (simple y seguro para persona mayor).
     * Si necesitas detectar cambios reales, guarda un snapshot previo y compáralo.
     */
    fun autoSaveIfDirty() {
        // Implementación simple: si hay algo en edición, guardamos.
        if (_state.value.editing != null) {
            saveEditing()
        }
    }

    /**
     * Elimina la nota por id y limpia la edición.
     * Si usas repositorio/BD, agrega la llamada correspondiente dentro del launch.
     */
    fun deleteNote(id: Long) {
        viewModelScope.launch {
            _state.update { s ->
                s.copy(
                    items = s.items.filterNot { it.id == id },
                    editing = null
                )
            }
        }
    }
}
