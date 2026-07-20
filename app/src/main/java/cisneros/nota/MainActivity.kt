@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cisneros.nota

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.max
import androidx.lifecycle.viewmodel.compose.viewModel
import cisneros.nota.data.NoteEntity
import cisneros.nota.settings.SettingsRepository
import cisneros.nota.ui.AccessibleEditorScreen
import cisneros.nota.ui.NoteListScreen
import cisneros.nota.ui.SeniorCalendarScreen
import cisneros.nota.ui.TextSizeLevel
import cisneros.nota.ui.theme.NotaTheme
import cisneros.nota.ui.toAccessibleSizes
import cisneros.nota.vm.NoteVm
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NotaTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    vm: NoteVm = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Preferencia de tamaño de texto (DataStore) — 0..4
    val settings = remember { SettingsRepository(context) }
    val sizeIdxFromStore by settings.bodySizeLevel.collectAsState(initial = 2)
    var textSize by remember(sizeIdxFromStore) {
        mutableStateOf(sizeIndexToLevel(sizeIdxFromStore))
    }
    val persistSize: (TextSizeLevel) -> Unit = { level ->
        textSize = level
        scope.launch { settings.setBodySizeLevel(levelToSizeIndex(level)) }
    }

    // Título personalizado para la libreta
    // Mostrar calendario (solo en modo compacto/medium)
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    // Compartir (WhatsApp → Business → chooser)
    val shareCurrentNote: () -> Unit = {
        state.editing?.let { shareNote(context, it.title, it.body) }
    }

    if (state.editing == null) {
        // Sin nota abierta: mostrar lista o calendario
        if (showCalendar) {
            ResponsivePane {
                CalendarScreen(
                    notes = state.items,
                    textSize = textSize,
                    onBackToList = { showCalendar = false }
                )
            }
        } else {
            ResponsivePane {
                NoteListScreen(
                    items = state.items,
                    results = state.results,
                    query = state.query,
                    onQuery = vm::setQuery,
                    onOpen = { id -> vm.edit(id) },
                    onAddNote = { vm.newNote() },
                    textSize = textSize,
                    onOpenCalendar = { showCalendar = true }
                )
            }
        }
    } else {
        // Pantalla de editor accesible
        ResponsivePane {
            EditorScaffold(
                onShare = shareCurrentNote,
                textSize = textSize,
                onChangeSize = persistSize
            ) {
                AccessibleEditorScreen(
                    vm = vm,
                    onBackToList = {
                        vm.autoSaveIfDirty()
                        vm.closeEditor()
                    },
                    textSize = textSize
                )
            }
        }
    }
}

@Composable
private fun ResponsivePane(
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sidePadding = when {
            maxWidth >= 1000.dp -> 40.dp
            maxWidth >= 700.dp -> 24.dp
            else -> 0.dp
        }
        val availableWidth = maxWidth - (sidePadding * 2)
        val contentWidth = max(availableWidth.value.coerceAtMost(820f), 0f).dp

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .width(contentWidth)
                    .fillMaxHeight()
            ) {
                content()
            }
        }
    }
}

/* ================== Scaffolds ================== */

@Composable
private fun EditorScaffold(
    onShare: () -> Unit,
    textSize: TextSizeLevel,
    onChangeSize: (TextSizeLevel) -> Unit,
    content: @Composable () -> Unit
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Editar", style = MaterialTheme.typography.titleMedium)
                    }
                },
                actions = {
                    IconButton(onClick = onShare, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Compartir nota",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Configuración",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    TextSizeMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        current = textSize,
                        onSelect = {
                            onChangeSize(it)
                            menuOpen = false
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content()
        }
    }
}

/**
 * Pantalla con AppBar para el calendario de notas.
 */
@Composable
private fun CalendarScreen(
    notes: List<NoteEntity>,
    textSize: TextSizeLevel,
    onBackToList: () -> Unit
) {
    val sizes = textSize.toAccessibleSizes()
    val cs = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "Calendario de notas",
                            fontSize = sizes.title
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToList, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = "Ver lista de notas",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        },
        containerColor = cs.background
    ) { padding ->
        SeniorCalendarScreen(
            notes = notes,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

/* ================== Menú tamaño de texto (5 niveles) ================== */

@Composable
private fun TextSizeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    current: TextSizeLevel,
    onSelect: (TextSizeLevel) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        MenuItem("Muy pequeño", TextSizeLevel.SMALLEST, current, onSelect)
        MenuItem("Pequeño",      TextSizeLevel.SMALLER,  current, onSelect)
        MenuItem("Normal",       TextSizeLevel.NORMAL,   current, onSelect)
        MenuItem("Grande",       TextSizeLevel.LARGER,   current, onSelect)
        MenuItem("Muy grande",   TextSizeLevel.LARGEST,  current, onSelect)
    }
}

@Composable
private fun MenuItem(
    label: String,
    level: TextSizeLevel,
    current: TextSizeLevel,
    onSelect: (TextSizeLevel) -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = { onSelect(level) },
        trailingIcon = {
            if (level == current) Icon(Icons.Filled.Check, contentDescription = "Seleccionado")
        }
    )
}

/* ================== Utilidades ================== */

private fun sizeIndexToLevel(idx: Int): TextSizeLevel = when (idx) {
    0 -> TextSizeLevel.SMALLEST
    1 -> TextSizeLevel.SMALLER
    3 -> TextSizeLevel.LARGER
    4 -> TextSizeLevel.LARGEST
    else -> TextSizeLevel.NORMAL
}
private fun levelToSizeIndex(level: TextSizeLevel): Int = when (level) {
    TextSizeLevel.SMALLEST -> 0
    TextSizeLevel.SMALLER  -> 1
    TextSizeLevel.NORMAL   -> 2
    TextSizeLevel.LARGER   -> 3
    TextSizeLevel.LARGEST  -> 4
}

// Compartir con WhatsApp (o Business) y fallback a chooser
private fun shareNote(context: Context, title: String, body: String) {
    val text = buildString {
        append(title.trim())
        if (body.isNotBlank()) {
            append("\n\n")
            append(body.trim())
        }
    }
    val base = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
    fun tryStart(pkg: String): Boolean = try {
        context.startActivity(
            Intent(base)
                .setPackage(pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    tryStart("com.whatsapp")
}
