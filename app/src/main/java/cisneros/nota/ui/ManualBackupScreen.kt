package cisneros.nota.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cisneros.nota.data.AppDb
import cisneros.nota.data.NoteBackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ManualBackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val service = remember(context) { NoteBackupService(AppDb.get(context)) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }

    val save = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                message = try {
                    val json = withContext(Dispatchers.IO) { service.exportJson() }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("No se pudo guardar el archivo")
                    }
                    "Copia guardada correctamente."
                } catch (e: Exception) { "No se pudo guardar la copia: ${e.localizedMessage ?: "error"}" }
                busy = false
            }
        }
    }
    val open = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                message = try {
                    pendingRestore = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use {
                            it.bufferedReader().readText()
                        } ?: error("No se pudo abrir el archivo")
                    }
                    "Revise la confirmación antes de recuperar sus notas."
                } catch (e: Exception) { "No se pudo leer la copia: ${e.localizedMessage ?: "error"}" }
                busy = false
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Copias de seguridad", style = MaterialTheme.typography.headlineMedium)
        Text("Guarde sus notas para recuperarlas si cambia de teléfono.", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = { save.launch("notas-jacc-respaldo.json") }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Guardar mis notas")
        }
        OutlinedButton(onClick = { open.launch(arrayOf("application/json", "text/plain")) }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Recuperar mis notas")
        }
        Text("La recuperación agrega las notas que falten. No borra las que ya tiene.")
        if (message.isNotBlank()) Text(message)
        TextButton(onClick = onBack) { Text("Volver a mis notas") }
    }
    if (pendingRestore != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("¿Recuperar las notas?") },
            text = { Text("Se agregarán las notas que falten. Sus notas actuales no se borrarán.") },
            confirmButton = {
                TextButton(onClick = {
                    val json = pendingRestore ?: return@TextButton
                    pendingRestore = null
                    busy = true
                    scope.launch {
                        message = try {
                            val count = withContext(Dispatchers.IO) { service.restoreJson(json) }
                            "Recuperación terminada: $count notas agregadas."
                        } catch (e: Exception) { "No se pudo recuperar la copia: ${e.localizedMessage ?: "archivo no válido"}" }
                        busy = false
                    }
                }) { Text("Sí, recuperar") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancelar") } }
        )
    }
}
