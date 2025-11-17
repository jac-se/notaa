package cisneros.nota.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cisneros.nota.vm.NoteVm

@Composable
fun AccessibleEditorScreen(
    vm: NoteVm,
    onBackToList: () -> Unit,
    textSize: TextSizeLevel
) {
    val state by vm.state.collectAsState()
    val editing = state.editing ?: return // Si no hay nota, no mostramos nada

    val sizes = textSize.toAccessibleSizes()
    val cs = MaterialTheme.colorScheme

    BackHandler {
        vm.autoSaveIfDirty()
        onBackToList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ===== Barra superior =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cs.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = {
                        vm.autoSaveIfDirty()
                        onBackToList()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver a la lista",
                        tint = cs.onPrimaryContainer
                    )
                }

                Text(
                    text = if (editing.id == null) "Nueva nota" else "Editar nota",
                    fontSize = sizes.title,
                    fontWeight = FontWeight.Bold,
                    color = cs.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )

                // Botón guardar
                IconButton(
                    onClick = {
                        vm.saveEditing()
                        onBackToList()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = "Guardar",
                        tint = cs.onPrimaryContainer
                    )
                }

                // Botón eliminar (solo si existe la nota)
                if (editing.id != null) {
                    IconButton(
                        onClick = {
                            vm.deleteNote(editing.id, hardDelete = false)
                            onBackToList()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Enviar a papelera",
                            tint = cs.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // ===== Campos de texto =====
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = editing.title,
                    onValueChange = { vm.updateEditing(title = it) },
                    label = { Text("Título (opcional)", fontSize = sizes.body) },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = sizes.title,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = editing.body,
                    onValueChange = { vm.updateEditing(body = it) },
                    label = { Text("Contenido", fontSize = sizes.body) },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = sizes.body
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 160.dp),
                    minLines = 6,
                    maxLines = Int.MAX_VALUE,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // ===== Acciones inferiores =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    vm.saveEditing()
                    onBackToList()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
            ) {
                Text("Guardar", fontSize = sizes.button, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    vm.autoSaveIfDirty()
                    onBackToList()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
            ) {
                Text("Cerrar", fontSize = sizes.button, fontWeight = FontWeight.Bold)
            }
        }
    }
}
