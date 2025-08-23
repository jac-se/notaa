@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class
)

package cisneros.nota

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.lifecycle.viewmodel.compose.viewModel
import cisneros.nota.settings.SettingsRepository
import cisneros.nota.ui.AccessibleEditorScreen
import cisneros.nota.ui.NoteListScreen
import cisneros.nota.ui.TextSizeLevel
import cisneros.nota.ui.theme.NotaTheme
import cisneros.nota.vm.NoteVm
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NotaTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val windowSize = calculateWindowSizeClass(this@MainActivity)
                    AppRoot(widthClass = windowSize.widthSizeClass)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    vm: NoteVm = viewModel(),
    widthClass: WindowWidthSizeClass
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Preferencia de tamaño de texto (DataStore) — 0..4
    val settings = remember { SettingsRepository(context) }
    val sizeIdxFromStore by settings.bodySizeLevel.collectAsState(initial = 2)
    var textSize by remember(sizeIdxFromStore) { mutableStateOf(sizeIndexToLevel(sizeIdxFromStore)) }
    val persistSize: (TextSizeLevel) -> Unit = { level ->
        textSize = level
        scope.launch { settings.setBodySizeLevel(levelToSizeIndex(level)) }
    }

    // Título personalizado para la libreta
    var customTitle by rememberSaveable { mutableStateOf("") }

    // Compartir (WhatsApp → Business → chooser)
    val shareCurrentNote: () -> Unit = {
        state.editing?.let { shareNote(context, it.title, it.body) }
    }

    // Expanded = dos paneles
    if (widthClass == WindowWidthSizeClass.Expanded && state.editing != null) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                // Simplificar: usar NoteListScreen directamente sin ListScaffold
                NoteListScreen(
                    items = state.items,
                    results = state.results,
                    query = state.query,
                    onQuery = vm::setQuery,
                    onOpen = { id -> vm.edit(id) },
                    onAddNote = { vm.newNote() },
                    textSize = textSize,
                    customTitle = customTitle,
                    onTitleChange = { customTitle = it }
                )
            }
            Box(Modifier.weight(1f)) {
                EditorScaffold(onShare = shareCurrentNote, textSize = textSize, onChangeSize = persistSize) {
                    AccessibleEditorScreen(
                        vm = vm,
                        onBackToList = { vm.autoSaveIfDirty(); vm.closeEditor() },
                        textSize = textSize
                    )
                }
            }
        }
        return
    }

    // Compact / Medium
    if (state.editing == null) {
        // Usar NoteListScreen directamente con título personalizable
        NoteListScreen(
            items = state.items,
            results = state.results,
            query = state.query,
            onQuery = vm::setQuery,
            onOpen = { id -> vm.edit(id) },
            onAddNote = { vm.newNote() },
            textSize = textSize,
            customTitle = customTitle,
            onTitleChange = { customTitle = it }
        )
    } else {
        EditorScaffold(onShare = shareCurrentNote, textSize = textSize, onChangeSize = persistSize) {
            AccessibleEditorScreen(
                vm = vm,
                onBackToList = { vm.autoSaveIfDirty(); vm.closeEditor() },
                textSize = textSize
            )
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
                        Icon(Icons.Filled.Share, contentDescription = "Compartir nota", modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configuración", modifier = Modifier.size(28.dp))
                    }
                    TextSizeMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        current = textSize,
                        onSelect = { onChangeSize(it); menuOpen = false }
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onShare,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartir", style = MaterialTheme.typography.bodyLarge)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
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

// DataStore ↔ enum (cinco niveles: 0..4)
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
        if (body.isNotBlank()) { append("\n\n"); append(body.trim()) }
    }
    val base = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
    fun tryStart(pkg: String): Boolean = try {
        context.startActivity(Intent(base).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) { false }

    val started = tryStart("com.whatsapp") || tryStart("com.whatsapp.w4b")
    if (!started) {
        val chooser = Intent.createChooser(base, "Compartir nota")
        context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}