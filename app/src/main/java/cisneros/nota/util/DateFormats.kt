package cisneros.nota.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object DateFormats {
    private val tz = TimeZone.getTimeZone("America/Mexico_City")
    private val localeMx = Locale("es", "MX")

    private val sameYearFmt = SimpleDateFormat("EEE d 'de' MMM, HH:mm", localeMx).apply {
        timeZone = tz
    }
    private val otherYearFmt = SimpleDateFormat("EEE d 'de' MMM 'de' yyyy, HH:mm", localeMx).apply {
        timeZone = tz
    }

    fun legibleOmitYear(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(tz, localeMx).apply { timeInMillis = epochMillis }
        val calNow = Calendar.getInstance(tz, localeMx).apply { timeInMillis = now }
        return if (cal.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)) {
            sameYearFmt.format(Date(epochMillis))
        } else {
            otherYearFmt.format(Date(epochMillis))
        }
    }

    fun relativa(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        return DateUtils.getRelativeTimeSpanString(
            epochMillis, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    /** Título por defecto para nuevas notas. */
    fun defaultTitle(now: Long = System.currentTimeMillis()): String = legibleOmitYear(now, now)
}
