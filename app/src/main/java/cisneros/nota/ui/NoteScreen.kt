package cisneros.nota.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cisneros.nota.data.NoteEntity
import java.text.SimpleDateFormat
import java.util.*

data class NoteUiState(
    val currentId: Long? = null,
    val title: String = "",
    val body: String = ""
)

fun NoteEntity?.toNoteUiState(): NoteUiState =
    if (this == null) NoteUiState() else NoteUiState(currentId = this.id, title = this.title, body = this.body)

private fun defaultNoteTitleNowMx(): String {
    val tz = TimeZone.getTimeZone("America/Mexico_City")
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))
    sdf.timeZone = tz
    return sdf.format(Date())
}

@Composable
fun NoteScreen(
    state: NoteUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    textSize: TextSizeLevel = TextSizeLevel.NORMAL
) {
    val sizes = textSize.toAccessibleSizes()

    LaunchedEffect(state.currentId) {
        if (state.currentId == null && state.title.isBlank()) {
            onTitleChange(defaultNoteTitleNowMx())
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = "✏️ ${if (state.currentId == null) "Nueva" else "Editando"} Nota",
                fontSize = sizes.title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    label = { Text("📅 Fecha y hora", fontSize = sizes.body, fontWeight = FontWeight.Medium) },
                    textStyle = LocalTextStyle.current.copy(fontSize = sizes.body, fontWeight = FontWeight.Medium),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.body,
                    onValueChange = onBodyChange,
                    label = { Text("📝 Escribe tu nota aquí", fontSize = sizes.body, fontWeight = FontWeight.Medium) },
                    textStyle = LocalTextStyle.current.copy(fontSize = sizes.body),
                    modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 200.dp),
                    minLines = 8,
                    maxLines = Int.MAX_VALUE
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f).heightIn(min = 70.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Guardar")
                Spacer(Modifier.width(12.dp))
                Text("💾 Guardar", fontSize = sizes.body, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f).heightIn(min = 70.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                Spacer(Modifier.width(12.dp))
                Text("❌ Cerrar", fontSize = sizes.body, fontWeight = FontWeight.Bold)
            }
        }
    }
}
