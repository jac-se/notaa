@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cisneros.nota.ui

// ---------- IMPORTS ----------

// Para usar nestedScroll en el Scaffold con scrollBehavior del TopAppBar
import androidx.compose.ui.input.nestedscroll.nestedScroll

// Para poder manejar foco (ej: cerrar teclado al presionar Done/Search)
import androidx.compose.ui.platform.LocalFocusManager

// Acciones del teclado virtual y opciones de entrada
import androidx.compose.foundation.text.KeyboardOptions   // <-- CORRECTO (no usar ui.text.input)
import androidx.compose.foundation.text.KeyboardActions  // Acciones cuando se presiona “Done” o “Search”

// Configuración del teclado virtual (IME)
import androidx.compose.ui.text.input.ImeAction              // Qué botón mostrar (Done, Search, Next…)
import androidx.compose.ui.text.input.KeyboardCapitalization // Auto-capitalización (ej: cada oración empieza en mayúscula)
import androidx.compose.ui.text.input.KeyboardType           // Tipo de teclado (Text, Number, Email, etc.)

// Para cortar textos largos con “...”
import androidx.compose.ui.text.style.TextOverflow

// Layouts y componentes básicos
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// Íconos de Material
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add

// Material 3
import androidx.compose.material3.*

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Para accesibilidad: describir elementos a TalkBack
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

// Tipografía
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Tu modelo de datos
import cisneros.nota.data.NoteEntity
import cisneros.nota.util.DateFormats

// ---------- COMPOSABLE ----------

@Composable
fun NoteListScreen(
    items: List<NoteEntity>,
    results: List<NoteEntity>,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (Long) -> Unit,
    onAddNote: () -> Unit,
    textSize: TextSizeLevel,
    // estado del título HOISTED desde el caller
    customTitle: String = "",
    onTitleChange: (String) -> Unit = {}
) {
    val sizes = textSize.toAccessibleSizes()
    val list = if (query.isBlank()) items else results
    val cs = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current // manejador de foco (para cerrar teclado)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp),
                            tint = cs.onSurface
                        )

                        // Campo de título editable (único TextField en la AppBar)
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = onTitleChange,
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp) // tamaño accesible para dedo grande
                                .semantics { contentDescription = "Título de la libreta" },
                            placeholder = {
                                Text(
                                    text = "Mi Libreta",
                                    fontSize = sizes.title,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = sizes.title,
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = cs.onSurface,
                                unfocusedTextColor = cs.onSurface
                            ),
                            // Configuración del teclado virtual para este campo
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done // botón "Listo"
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focus.clearFocus() } // cerrar teclado al presionar Done
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.surface,
                    titleContentColor = cs.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva nota", fontSize = sizes.body) },
                modifier = Modifier.semantics { contentDescription = "Nueva nota" },
                containerColor = cs.primary,
                contentColor = cs.onPrimary
            )
        },
        containerColor = cs.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Campo de búsqueda (fuera del TopAppBar para no pelear con el título)
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .heightIn(min = 56.dp)
                    .semantics { contentDescription = "Buscar notas" },
                placeholder = {
                    Text(
                        text = "Buscar…",
                        fontSize = sizes.body,
                        color = cs.onSurfaceVariant
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = sizes.body,
                    color = cs.onSurface
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outline,
                    focusedContainerColor = cs.surface,
                    unfocusedContainerColor = cs.surface,
                    cursorColor = cs.primary,
                    focusedTextColor = cs.onSurface,
                    unfocusedTextColor = cs.onSurface
                ),
                // Configuración del teclado virtual para el buscador
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search // botón "Buscar"
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { focus.clearFocus() } // cerrar teclado al presionar Buscar
                )
            )

            Spacer(Modifier.height(4.dp))

            if (list.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isBlank()) "No hay notas aún" else "No se encontraron notas",
                        fontSize = sizes.body,
                        color = cs.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.id }) { note ->
                        val displayTitle = note.title.takeIf { it.isNotBlank() } ?: "Sin título"
                        val legible = DateFormats.legibleOmitYear(note.createdAt)
                        val rel = DateFormats.relativa(note.createdAt)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { onOpen(note.id) }
                                .semantics { contentDescription = "Nota: $displayTitle" },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = cs.surfaceVariant,
                                contentColor = cs.onSurfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = displayTitle,
                                    fontSize = sizes.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "$legible · $rel",
                                    fontSize = sizes.body,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
