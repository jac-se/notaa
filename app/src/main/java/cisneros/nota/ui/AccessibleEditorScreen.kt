package cisneros.nota.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cisneros.nota.vm.NoteVm
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccessibleEditorScreen(
    vm: NoteVm,
    onBackToList: () -> Unit,
    textSize: TextSizeLevel = TextSizeLevel.NORMAL
) {
    val state by vm.state.collectAsState()
    val editing = state.editing ?: return
    val sizes = textSize.toAccessibleSizes()

    // Prefijar título con fecha/hora de México si es nota nueva y sin título
    LaunchedEffect(editing.id) {
        if (editing.id == 0L && editing.title.isBlank()) {
            val tz = TimeZone.getTimeZone("America/Mexico_City")
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))
            sdf.timeZone = tz
            vm.updateEditing(title = sdf.format(Date()))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ===== Barra superior =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Volver
                Button(
                    onClick = { vm.autoSaveIfDirty(); onBackToList() },
                    modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        Spacer(Modifier.width(8.dp))
                        Text("Volver", fontSize = sizes.button, fontWeight = FontWeight.Bold)
                    }
                }

                if (editing.id > 0L) {
                    Button(
                        onClick = { vm.deleteNote(editing.id); onBackToList() },
                        modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminar", fontSize = sizes.button, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = { vm.saveEditing(); onBackToList() },
                        modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar")
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar", fontSize = sizes.button, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ===== Editor =====
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "✏️ Editando nota",
                    fontSize = sizes.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editing.title,
                    onValueChange = { vm.updateEditing(title = it) },
                    label = { Text("Título de la nota", fontSize = sizes.body) },
                    textStyle = LocalTextStyle.current.copy(fontSize = sizes.body, fontWeight = FontWeight.Medium),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = editing.body,
                    onValueChange = { vm.updateEditing(body = it) },
                    label = { Text("Contenido de la nota", fontSize = sizes.body) },
                    textStyle = LocalTextStyle.current.copy(fontSize = sizes.body),
                    modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 160.dp),
                    minLines = 6,
                    maxLines = Int.MAX_VALUE,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }

    BackHandler {
        vm.autoSaveIfDirty()
        onBackToList()
    }
}
