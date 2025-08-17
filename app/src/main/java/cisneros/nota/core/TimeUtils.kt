package cisneros.nota.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_MX = Locale("es", "MX")
private val ZONE = ZoneId.systemDefault()

// sábado, 17 de agosto de 2025, 10:45 a. m.
private val FULL_FMT_MX: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy, hh:mm a", LOCALE_MX)
        .withZone(ZONE)

// Para usar en el título por defecto (mismo formato)
fun defaultNoteTitleNow(): String =
    LocalDateTime.now().format(FULL_FMT_MX)

// Para formatear un timestamp (ms) al texto anterior
fun formatMx(tsMillis: Long): String =
    FULL_FMT_MX.format(Instant.ofEpochMilli(tsMillis))
