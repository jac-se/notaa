@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cisneros.nota.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp),
                            tint = cs.onSurface
                        )
                        Text(
                            text = "Notas",
                            fontSize = sizes.title,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
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
            FloatingActionButton(
                onClick = onAddNote,
                modifier = Modifier
                    .padding(16.dp)
                    .semantics { contentDescription = "Nueva nota" },
                containerColor = cs.primary,
                contentColor = cs.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
        containerColor = cs.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Buscar nota",
                fontSize = sizes.body,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 10.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
                    .heightIn(min = 64.dp)
                    .semantics { contentDescription = "Buscar notas" },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = cs.onSurfaceVariant
                    )
                },
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
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.None,
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
                        .imePadding()
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
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.id }) { note ->
                        val displayTitle = note.visibleTitle()
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
