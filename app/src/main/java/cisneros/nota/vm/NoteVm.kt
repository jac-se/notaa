package cisneros.nota.vm

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cisneros.nota.data.AppDb
import cisneros.nota.data.NoteEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class UiState(
    val items: List<NoteEntity> = emptyList(),
    val editing: NoteEntity? = null,
    val query: String = "",
    val searching: Boolean = false,
    val results: List<NoteEntity> = emptyList()
)

class NoteVm(app: Application) : AndroidViewModel(app) {


    // ---- DB/DAO ----
    private val db = AppDb.get(app)
    private val dao = db.noteDao()

    // ---- Estado ----
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // ---- Autosave ----
    private var autoSaveJob: Job? = null

    // ---- Formatos fecha (CDMX, es-MX) ----
    private val tz: TimeZone = TimeZone.getTimeZone("America/Mexico_City")
    private val localeMx = Locale("es", "MX")
    private val sameYearFmt = SimpleDateFormat("EEE d 'de' MMM, HH:mm", localeMx).apply { timeZone = tz }
    private val otherYearFmt = SimpleDateFormat("EEE d 'de' MMM 'de' yyyy, HH:mm", localeMx).apply { timeZone = tz }

    init {
        // Stream de notas en tiempo real
        viewModelScope.launch {
            dao.streamAll().collectLatest { list ->
                _state.update { it.copy(items = list) }
            }
        }
    }

    // -------------------------
    // CRUD + edición
    // -------------------------

    /** Inicia una nueva nota en edición con título autogenerado (fecha legible). */
    fun newNote() {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            // Ajusta nombres/orden según tu entidad real:
            // id por defecto = 0L
            title = defaultTitle(now),
            body = "",
            createdAt = now
        )
        _state.update { it.copy(editing = note) }
    }

    /** Carga a edición la nota con id dado. */
    fun edit(id: Long) {
        viewModelScope.launch {
            val n = dao.getById(id)
            _state.update { it.copy(editing = n) }
        }
    }

    /** Actualiza campos de la nota en edición (solo en memoria). */
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

    /** Guarda la nota en edición (upsert). */
    fun saveEditing() {
        val e = _state.value.editing ?: return
        viewModelScope.launch {
            val id = upsertFromVm(e)
            val refreshed = dao.getById(id) ?: e.copy(id = id)
            _state.update { it.copy(editing = refreshed) }
        }
    }

    /** Envía a papelera (o elimina lógico según tu DAO). */
    fun delete(id: Long) {
        viewModelScope.launch {
            dao.moveToTrash(id, System.currentTimeMillis())
        }
    }

    // -------------------------
    // Autosave (debounce)
    // -------------------------

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

    // -------------------------
    // Búsqueda
    // -------------------------

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
        search(q)
    }

    private fun search(q: String) {
        viewModelScope.launch {
            if (q.isBlank()) {
                _state.update { it.copy(searching = false, results = emptyList()) }
                return@launch
            }
            _state.update { it.copy(searching = true) }
            val res = dao.search("%${q.trim()}%") // title LIKE o body LIKE en DAO
            _state.update { it.copy(searching = false, results = res) }
        }
    }

    // -------------------------
    // Fechas y título
    // -------------------------

    /** Fecha legible omitiendo el año si es el actual. */
    fun legibleOmitYear(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(tz, localeMx).apply { timeInMillis = epochMillis }
        val calNow = Calendar.getInstance(tz, localeMx).apply { timeInMillis = now }
        return if (cal.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)) {
            sameYearFmt.format(Date(epochMillis))
        } else {
            otherYearFmt.format(Date(epochMillis))
        }
    }

    /** Fecha relativa (“hace 3 min”, “hace 2 h”…). */
    fun relativa(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        return DateUtils.getRelativeTimeSpanString(
            epochMillis, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    /** Título por defecto (fecha legible). */
    fun defaultTitle(now: Long = System.currentTimeMillis()): String = legibleOmitYear(now, now)

    // -------------------------
    // Upsert desde VM (sin updatedAt)
    // -------------------------

    private suspend fun upsertFromVm(n: NoteEntity): Long {
        return if (n.id == 0L) {
            val created = if (n.createdAt > 0) n.createdAt else System.currentTimeMillis()
            dao.insert(n.copy(createdAt = created))
        } else {
            dao.update(n)
            n.id
        }
    }
    fun deleteNote(id: Long, hardDelete: Boolean = false) {
        viewModelScope.launch {
            if (hardDelete) {
                dao.deleteById(id)              // borrado definitivo
            } else {
                dao.moveToTrash(id, System.currentTimeMillis()) // papelera
            }
        }
    }
    fun closeEditor() {
        _state.update { it.copy(editing = null) }
    }

}
