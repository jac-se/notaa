package cisneros.nota.work

import cisneros.nota.data.ReminderRepeat
import java.time.Instant
import java.time.ZoneId

object ReminderScheduleCalculator {
    fun nextTrigger(triggerAt: Long, repeat: ReminderRepeat): Long {
        val dateTime = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault())
        return when (repeat) {
            ReminderRepeat.DAILY -> dateTime.plusDays(1).toInstant().toEpochMilli()
            ReminderRepeat.WEEKLY -> dateTime.plusWeeks(1).toInstant().toEpochMilli()
            ReminderRepeat.NONE -> triggerAt
        }
    }
}
