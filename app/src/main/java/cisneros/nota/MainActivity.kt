package cisneros.nota

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cisneros.nota.ui.AccessibleEditorScreen
import cisneros.nota.ui.NoteListScreen
import cisneros.nota.ui.TextSizeLevel
import cisneros.nota.vm.NoteVm
import cisneros.nota.ui.NoteItem

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    // IMPORTANTE: estos dos imports deben existir arriba
                    // import androidx.lifecycle.viewmodel.compose.viewModel
                    // import androidx.compose.runtime.collectAsState

                    val vm: NoteVm = viewModel()
                    val nav = rememberNavController()

                    // Colección del StateFlow de tu VM
                    val uiState by vm.state.collectAsState()

                    var currentTextSize by remember { mutableStateOf(TextSizeLevel.NORMAL) }
                    var showTextSizeDialog by remember { mutableStateOf(false) }

                    NavHost(navController = nav, startDestination = "list") {
                        composable("list") {
                            Box(modifier = Modifier.fillMaxSize()) {
                                NoteListScreen(
                                    notes = uiState.items.map {
                                        NoteItem(it.id, it.title.ifBlank { "Sin título" })
                                    },
                                    onAddNote = { text: String ->
                                        vm.newNote()
                                        if (text.isNotBlank()) vm.updateEditing(body = text)
                                        nav.navigate("edit")
                                    },
                                    onOpen = { id: Long ->
                                        vm.edit(id)
                                        nav.navigate("edit")
                                    }
                                )

                                FloatingActionButton(
                                    onClick = { showTextSizeDialog = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp),
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Tamaño de texto"
                                    )
                                }
                            }
                        }
                        composable("edit") {
                            AccessibleEditorScreen(
                                vm = vm,
                                onBackToList = {
                                    vm.autoSaveIfDirty()
                                    nav.popBackStack()
                                },
                                textSize = currentTextSize
                            )
                        }
                    }

                    if (showTextSizeDialog) {
                        TextSizeDialog(
                            currentSize = currentTextSize,
                            onSizeSelected = { newSize ->
                                currentTextSize = newSize
                                showTextSizeDialog = false
                            },
                            onDismiss = { showTextSizeDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextSizeDialog(
    currentSize: TextSizeLevel,
    onSizeSelected: (TextSizeLevel) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔤 Tamaño de Texto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Selecciona el tamaño de texto más cómodo para ti:")
                TextSizeLevel.values().forEach { size ->
                    val isSelected = size == currentSize
                    val fontSize = when (size) {
                        TextSizeLevel.SMALLEST -> 14.sp
                        TextSizeLevel.SMALLER  -> 16.sp
                        TextSizeLevel.NORMAL   -> 18.sp
                        TextSizeLevel.LARGER   -> 20.sp
                        TextSizeLevel.LARGEST  -> 22.sp
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        onClick = { onSizeSelected(size) }
                    ) {
                        Text(
                            text = "${getSizeLabel(size)} - Texto de ejemplo",
                            modifier = Modifier.padding(16.dp),
                            fontSize = fontSize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun getSizeLabel(size: TextSizeLevel): String = when (size) {
    TextSizeLevel.SMALLEST -> "🔸 Muy pequeño"
    TextSizeLevel.SMALLER  -> "🔹 Pequeño"
    TextSizeLevel.NORMAL   -> "🔵 Normal"
    TextSizeLevel.LARGER   -> "🔶 Grande"
    TextSizeLevel.LARGEST  -> "🟠 Muy grande"
}
