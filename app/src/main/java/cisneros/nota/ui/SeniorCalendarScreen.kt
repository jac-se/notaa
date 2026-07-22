package cisneros.nota.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cisneros.nota.data.NoteEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendario sencillo y de letra grande, pensado para personas mayores.
 * Muestra un mes, resalta los días que tienen notas y lista las notas del día seleccionado.
 */
@Composable
fun SeniorCalendarScreen(
    notes: List<NoteEntity>,
    modifier: Modifier = Modifier
) {
    val zoneMx = remember { ZoneId.of("America/Mexico_City") }
    val localeMx = remember { Locale.of("es", "MX") }

    // Agrupar notas por fecha (solo año-mes-día)
    val notesByDate: Map<LocalDate, List<NoteEntity>> = remember(notes) {
        notes.groupBy { note ->
            Instant.ofEpochMilli(note.createdAt)
                .atZone(zoneMx)
                .toLocalDate()
        }
    }

    var currentMonth by remember { mutableStateOf(YearMonth.now(zoneMx)) }
    var selectedDate by remember { mutableStateOf(LocalDate.now(zoneMx)) }

    val daysOfWeek = remember { DayOfWeek.values().toList() }
    val notesForSelectedDay = notesByDate[selectedDate].orEmpty()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado de mes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "<",
                fontSize = 28.sp,
                modifier = Modifier
                    .clickable {
                        currentMonth = currentMonth.minusMonths(1)
                        selectedDate = currentMonth.atDay(1)
                    }
                    .padding(8.dp)
            )

            Text(
                text = currentMonth.month.getDisplayName(TextStyle.FULL, localeMx)
                    .replaceFirstChar { it.uppercase(localeMx) } +
                        " ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = ">",
                fontSize = 28.sp,
                modifier = Modifier
                    .clickable {
                        currentMonth = currentMonth.plusMonths(1)
                        selectedDate = currentMonth.atDay(1)
                    }
                    .padding(8.dp)
            )
        }

        // Nombres de días (L, M, X...)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { dow ->
                val short = dow.getDisplayName(TextStyle.SHORT, localeMx)
                Text(
                    text = short.first().uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Celdas del calendario
        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeekIndex = (firstDayOfMonth.dayOfWeek.value % 7)
        val lengthOfMonth = currentMonth.lengthOfMonth()

        val days: List<LocalDate?> = buildList {
            repeat(firstDayOfWeekIndex) { add(null) }
            for (day in 1..lengthOfMonth) {
                add(currentMonth.atDay(day))
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(days) { date ->
                if (date == null) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                    )
                } else {
                    val hasNotes = notesByDate.containsKey(date)
                    val isSelected = date == selectedDate

                    val bgColor: Color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        hasNotes -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val textColor: Color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clickable { selectedDate = date },
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        // Lista de notas del día seleccionado
        if (notesForSelectedDay.isEmpty()) {
            Text(
                text = "Sin notas para esta fecha",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Notas del ${selectedDate.dayOfMonth} " +
                            currentMonth.month.getDisplayName(TextStyle.SHORT, localeMx),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                notesForSelectedDay.forEach { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = note.title.ifBlank { "Sin título" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (note.body.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = note.body.take(80),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
