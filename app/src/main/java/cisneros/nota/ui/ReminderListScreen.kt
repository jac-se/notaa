@file:OptIn(ExperimentalLayoutApi::class)

package cisneros.nota.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cisneros.nota.data.ReminderEntity
import cisneros.nota.data.ReminderRepeat
import cisneros.nota.data.displayTitle
import cisneros.nota.util.DateFormats
import java.util.Calendar

@Composable
fun ReminderListScreen(
    reminders: List<ReminderEntity>,
    textSize: TextSizeLevel,
    onBack: () -> Unit,
    onSave: (ReminderEntity) -> Unit,
    onComplete: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onTestNotifications: () -> Unit = {},
) {
    val sizes = textSize.toAccessibleSizes()
    var editing by remember { mutableStateOf<ReminderEntity?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ReminderEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Recordatorios", fontSize = sizes.title, fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text("Notas", fontSize = sizes.body)
            }
            OutlinedButton(
                onClick = onTestNotifications,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text("Probar avisos", fontSize = sizes.body)
            }
            Button(
                onClick = { creating = true },
                modifier = Modifier.weight(1.2f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text("Nuevo aviso", fontSize = sizes.body)
            }
        }
        if (reminders.isEmpty()) {
            Text(
                "No hay recordatorios pendientes.",
                fontSize = sizes.body,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(reminders, key = { it.id }) { reminder ->
                    val title = reminder.displayTitle()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Recordatorio: $title" },
                        colors = CardDefaults.cardColors()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                title,
                                fontSize = sizes.title,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (reminder.description.isNotBlank()) {
                                Text(reminder.description, fontSize = sizes.body)
                            }
                            Text(
                                if (reminder.completed) "Completado"
                                else DateFormats.legibleOmitYear(reminder.triggerAt) + " · " + repeatLabel(reminder.repeat),
                                fontSize = sizes.body,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { editing = reminder },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text("Editar", fontSize = sizes.body)
                                }
                                if (!reminder.completed) {
                                    Button(
                                        onClick = { onComplete(reminder.id) },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text("Completar", fontSize = sizes.body)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { deleting = reminder },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text("Eliminar", fontSize = sizes.body)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        ReminderEditorDialog(
            initial = editing,
            textSize = textSize,
            onDismiss = { creating = false; editing = null },
            onSave = { onSave(it); creating = false; editing = null },
        )
    }
    deleting?.let { reminder ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("¿Eliminar recordatorio?", fontSize = sizes.title) },
            text = { Text("Esta acción no se puede deshacer.", fontSize = sizes.body) },
            confirmButton = {
                Button(onClick = { onDelete(reminder.id); deleting = null }) {
                    Text("Eliminar", fontSize = sizes.body)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text("Cancelar", fontSize = sizes.body)
                }
            },
        )
    }
}

@Composable
private fun ReminderEditorDialog(
    initial: ReminderEntity?,
    textSize: TextSizeLevel,
    onDismiss: () -> Unit,
    onSave: (ReminderEntity) -> Unit,
) {
    val context = LocalContext.current
    val sizes = textSize.toAccessibleSizes()
    val key = initial?.id ?: 0L
    var category by remember(key) { mutableStateOf(initial?.category.orEmpty()) }
    var description by remember(key) { mutableStateOf(initial?.description.orEmpty()) }
    var triggerAt by remember(key) { mutableStateOf(initial?.triggerAt ?: (System.currentTimeMillis() + 3_600_000L)) }
    var repeat by remember(key) { mutableStateOf(initial?.repeat ?: ReminderRepeat.NONE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) "Nuevo recordatorio" else "Editar recordatorio",
                fontSize = sizes.title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("¿Qué desea recordar?", fontSize = sizes.body) },
                    placeholder = {
                        Text(
                            "Ej. Pago, compra, cita, medicamento, llamada… (opcional)",
                            fontSize = sizes.body
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción o detalles (opcional)", fontSize = sizes.body) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = triggerAt }
                        DatePickerDialog(context, { _, year, month, day ->
                            TimePickerDialog(context, { _, hour, minute ->
                                calendar.set(year, month, day, hour, minute, 0)
                                triggerAt = calendar.timeInMillis
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text("Fecha y hora: " + DateFormats.legibleOmitYear(triggerAt), fontSize = sizes.body)
                }
                Text("Repetición:", fontSize = sizes.body, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeatChoices.forEach { option ->
                        val selected = repeat == option
                        if (selected) {
                            Button(onClick = { repeat = option }) {
                                Text("✓ " + repeatLabel(option), fontSize = sizes.body)
                            }
                        } else {
                            OutlinedButton(onClick = { repeat = option }) {
                                Text(repeatLabel(option), fontSize = sizes.body)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (triggerAt > 0) {
                        onSave(
                            ReminderEntity(
                                id = initial?.id ?: 0,
                                category = category.trim(),
                                description = description.trim(),
                                triggerAt = triggerAt,
                                repeat = repeat,
                                completed = false,
                                createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                            )
                        )
                    }
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("Guardar recordatorio", fontSize = sizes.body)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontSize = sizes.body)
            }
        },
    )
}

private val repeatChoices = listOf(ReminderRepeat.NONE, ReminderRepeat.DAILY, ReminderRepeat.WEEKLY)

private fun repeatLabel(repeat: ReminderRepeat) = when (repeat) {
    ReminderRepeat.NONE -> "Una vez"
    ReminderRepeat.DAILY -> "Diario"
    ReminderRepeat.WEEKLY -> "Semanal"
}
