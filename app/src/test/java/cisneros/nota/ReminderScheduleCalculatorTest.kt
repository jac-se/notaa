package cisneros.nota

import cisneros.nota.data.ReminderRepeat
import cisneros.nota.work.ReminderScheduleCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class ReminderScheduleCalculatorTest {
    @Test
    fun dailyReminderMovesOneCalendarDay() {
        val start = Instant.parse("2026-01-10T08:00:00Z").toEpochMilli()
        val expected = Instant.parse("2026-01-11T08:00:00Z").toEpochMilli()
        assertEquals(expected, ReminderScheduleCalculator.nextTrigger(start, ReminderRepeat.DAILY))
    }

    @Test
    fun weeklyReminderMovesOneCalendarWeek() {
        val start = Instant.parse("2026-01-10T08:00:00Z").toEpochMilli()
        val expected = Instant.parse("2026-01-17T08:00:00Z").toEpochMilli()
        assertEquals(expected, ReminderScheduleCalculator.nextTrigger(start, ReminderRepeat.WEEKLY))
    }
}
