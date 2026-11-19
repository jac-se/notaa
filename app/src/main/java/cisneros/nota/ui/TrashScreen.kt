@file:OptIn(ExperimentalLayoutApi::class)

package cisneros.nota.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cisneros.nota.data.NoteEntity
import cisneros.nota.util.DateFormats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    trashItems: List<NoteEntity>,
    textSize: TextSizeLevel,
    onBack: () -> Unit,
    onRestore: (Long) -> Unit,
    onDeletePermanently: (Long) -> Unit,
    onEmptyTrash: () -> Unit,
) {
    val sizes = textSize.toAccessibleSizes()
    val cs = MaterialTheme.colorScheme

    var noteToDeletePermanently by remember { mutableStateOf<NoteEntity?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.RestoreFromTrash,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Papelera de notas",
                            fontSize = sizes.title,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a la lista de notas"
                        )
                    }
                },
                actions = {
                    if (trashItems.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Vaciar la papelera"
                            )
                        }
                    }
                }
            )
        },
        containerColor = cs.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (trashItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.RestoreFromTrash,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "La papelera está vacía.",
                            fontSize = sizes.title,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "Notas eliminadas. Puede recuperarlas o borrarlas definitivamente.",
                    fontSize = sizes.body,
                    color = cs.onSurfaceVariant
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(trashItems, key = { it.id }) { note ->
                        val displayTitle = note.visibleTitle()
                        val deletedDateText = note.deletedAt?.let { DateFormats.legibleOmitYear(it) }.orEmpty()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Nota eliminada: $displayTitle" },
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = cs.surfaceVariant,
                                contentColor = cs.onSurfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = displayTitle,
                                    fontSize = sizes.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (note.body.isNotBlank()) {
                                    Text(
                                        text = note.body,
                                        fontSize = sizes.body,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (deletedDateText.isNotBlank()) {
                                    Text(
                                        text = "Eliminada el: $deletedDateText",
                                        fontSize = sizes.body,
                                        color = cs.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { onRestore(note.id) },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text("Recuperar nota", fontSize = sizes.body, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { noteToDeletePermanently = note },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text("Borrar definitivamente", fontSize = sizes.body)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo confirmación para borrar definitivamente una nota
    noteToDeletePermanently?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDeletePermanently = null },
            title = { Text("¿Borrar permanentemente?", fontSize = sizes.title) },
            text = { Text("Esta nota se eliminará definitivamente de la memoria. Esta acción no se puede deshacer.", fontSize = sizes.body) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePermanently(note.id)
                        noteToDeletePermanently = null
                    }
                ) {
                    Text("Borrar definitivamente", fontSize = sizes.body)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDeletePermanently = null }) {
                    Text("Cancelar", fontSize = sizes.body)
                }
            }
        )
    }

    // Diálogo confirmación para vaciar la papelera
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("¿Vaciar la papelera?", fontSize = sizes.title) },
            text = { Text("Se eliminarán de forma permanente todas las notas que están en la papelera.", fontSize = sizes.body) },
            confirmButton = {
                Button(
                    onClick = {
                        onEmptyTrash()
                        showEmptyTrashDialog = false
                    }
                ) {
                    Text("Vaciar papelera", fontSize = sizes.body)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancelar", fontSize = sizes.body)
                }
            }
        )
    }
}

private fun NoteEntity.visibleTitle(): String {
    val bodyPreview = body
        .trim()
        .replace(Regex("\\s+"), " ")

    return when {
        title.isNotBlank() -> title
        bodyPreview.isNotBlank() -> bodyPreview.take(40)
        else -> "Sin título"
    }
}
