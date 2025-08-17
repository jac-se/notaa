@file:OptIn(ExperimentalMaterial3Api::class)

package cisneros.nota.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cisneros.nota.data.NoteEntity
import cisneros.nota.vm.NoteVm
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla principal (para adultos mayores):
 * - **Editor como Home**.
 * - **Título por defecto** = fecha/hora CDMX (viene del VM).
 * - **Letra grande** por defecto con controles **A-/A+**.
 * - **Botones grandes** (Guardar / Compartir WhatsApp).
 * - **Autoguardado**: cada 60s y tras 800ms de inactividad al escribir.
 */
@Composable
fun App(vm: NoteVm) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    // Tamaños de fuente ajustables (persistencia simple en memoria)
    var baseBodySp by remember { mutableStateOf(18.sp) }   // grande por defecto
    var baseTitleSp by remember { mutableStateOf(22.sp) }  // más grande para título

    // Editor muestra siempre la nota actual (si hubiera null, crea placeholder local)
    val note = state.editing ?: NoteEntity(title = "", body = "")

    // Zona/locale MX para mostrar en header (informativo)
    val zoneMx = remember { ZoneId.of("America/Mexico_City") }
    val localeMx = remember { Locale("es", "MX") }
    val dateOnlyFmt = remember { DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", localeMx) }
    val timeOnlyFmt = remember { DateTimeFormatter.ofPattern("HH:mm", localeMx) }
    var now by remember { mutableStateOf(ZonedDateTime.now(zoneMx)) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now(zoneMx)
            delay(30_000)
        }
    }

    // AUTOGUARDADO 1: cada 60s
    LaunchedEffect(state.editing) {
        while (true) {
            delay(60_000)
            vm.autoSaveIfDirty()
        }
    }

    // UI
    MaterialTheme(
        typography = Typography(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = baseBodySp),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontSize = baseTitleSp),
            labelLarge = MaterialTheme.typography.labelLarge.copy(fontSize = baseBodySp)
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // Encabezado informativo (la nota ya trae este título por defecto)
                            Text(
                                text = now.format(dateOnlyFmt).replaceFirstChar { it.uppercase(localeMx) },
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Hora: ${now.format(timeOnlyFmt)} (CDMX)",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    },
                    actions = {
                        // A-/A+ para ajustar tamaño
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    baseBodySp = (baseBodySp.value - 2).coerceAtLeast(14f).sp
                                    baseTitleSp = (baseTitleSp.value - 2).coerceAtLeast(18f).sp
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) { Text("A-") }
                            OutlinedButton(
                                onClick = {
                                    baseBodySp = (baseBodySp.value + 2).coerceAtMost(28f).sp
                                    baseTitleSp = (baseTitleSp.value + 2).coerceAtMost(34f).sp
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) { Text("A+") }
                            // Guardar grande
                            Button(
                                onClick = { vm.saveEditing() },
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) { Text("Guardar") }
                        }
                    }
                )
            }
        ) { padding ->
            NoteEditorLarge(
                note = note,
                onTitle = { vm.updateEditing(title = it) },
                onBody = { vm.updateEditing(body = it) },
                onIdle = { vm.autoSaveIfDirty() }, // AUTOGUARDADO 2: debounce al escribir
                onShareWhats = {
                    val text = buildShareText(note)
                    val wa = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        // Forzamos WhatsApp si está; si no, caerá al chooser
                        `package` = "com.whatsapp"
                    }
                    try {
                        ctx.startActivity(wa)
                    } catch (_: ActivityNotFoundException) {
                        // Fallback: chooser genérico
                        val chooser = Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            },
                            "Compartir nota"
                        )
                        ctx.startActivity(chooser)
                    }
                },
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                bodySp = baseBodySp,
                titleSp = baseTitleSp
            )
        }
    }
}

/** Construye el texto para compartir: Título + línea en blanco + Cuerpo. */
private fun buildShareText(note: NoteEntity): String =
    buildString {
        appendLine(note.title.ifBlank { "Nota" })
        appendLine()
        append(note.body)
    }

/**
 * Editor accesible (letra grande) para una nota.
 *
 * @param note        Nota a editar.
 * @param onTitle     Callback al cambiar título.
 * @param onBody      Callback al cambiar contenido.
 * @param onIdle      Se dispara tras breve inactividad para autoguardado.
 * @param onShareWhats Compartir por WhatsApp como texto.
 * @param modifier    Modificador exterior.
 * @param bodySp      Tamaño base del cuerpo.
 * @param titleSp     Tamaño base del título.
 */
@Composable
private fun NoteEditorLarge(
    note: NoteEntity,
    onTitle: (String) -> Unit,
    onBody: (String) -> Unit,
    onIdle: () -> Unit,
    onShareWhats: () -> Unit,
    modifier: Modifier = Modifier,
    bodySp: androidx.compose.ui.unit.TextUnit = 18.sp,
    titleSp: androidx.compose.ui.unit.TextUnit = 22.sp
) {
    var title by remember(note.id) { mutableStateOf(TextFieldValue(note.title)) }
    var body by remember(note.id) { mutableStateOf(TextFieldValue(note.body)) }

    // Propaga cambios al VM
    LaunchedEffect(title.text) { onTitle(title.text) }
    LaunchedEffect(body.text) { onBody(body.text) }

    // Debounce simple: si el usuario deja de teclear 800ms, autoguardamos
    LaunchedEffect(title.text, body.text) {
        delay(800)
        onIdle()
    }

    Column(modifier) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título (por defecto: fecha/hora CDMX)") },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = titleSp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Contenido de la nota") },
            textStyle = LocalTextStyle.current.copy(fontSize = bodySp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Spacer(Modifier.height(12.dp))
        // Botones grandes y claros
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onShareWhats,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("WhatsApp") }
            // Dejamos un segundo botón para acciones futuras (por ahora no hace nada)
            OutlinedButton(
                onClick = { /* reservado */ },
                enabled = false,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Opciones") }
        }
    }
}
