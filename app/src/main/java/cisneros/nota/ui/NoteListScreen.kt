@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cisneros.nota.ui

// ---------- IMPORTS ----------

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cisneros.nota.data.NoteEntity
import cisneros.nota.util.DateFormats

@Composable
fun NoteListScreen(
    items: List<NoteEntity>,
    results: List<NoteEntity>,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (Long) -> Unit,
    onAddNote: () -> Unit,
    textSize: TextSizeLevel,
    customTitle: String = "",
    onTitleChange: (String) -> Unit = {},
    onOpenCalendar: () -> Unit = {}
) {
    val sizes = textSize.toAccessibleSizes()
    val list = if (query.isBlank()) items else results
    val cs = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current

    val scrollBehavior = pinnedScrollBehavior()

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

                        // Título editable de la libreta
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = onTitleChange,
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
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
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focus.clearFocus() }
                            )
                        )
                    }
                },
                actions = {
                    // 🔔 Botón de calendario ARRIBA a la derecha
                    IconButton(
                        onClick = onOpenCalendar,
                        modifier = Modifier.semantics {
                            contentDescription = "Ver calendario de notas"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = cs.onSurface
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
            // Solo FAB de "Nueva nota" (el calendario ya está arriba)
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva nota", fontSize = sizes.button) },
                modifier = Modifier
                    .padding(16.dp)
                    .semantics { contentDescription = "Nueva nota" },
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
            // Campo de búsqueda
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { focus.clearFocus() }
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
