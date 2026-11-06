package cisneros.nota

import cisneros.nota.data.ReminderEntity
import cisneros.nota.data.ReminderRepeat
import cisneros.nota.data.displayTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTest {

    @Test
    fun displayTitleReturnsMiRecordatorioWhenTitleIsEmptyOrBlank() {
        val emptyReminder = ReminderEntity(category = "", description = "", triggerAt = 1000L)
        val blankReminder = ReminderEntity(category = "   ", description = "", triggerAt = 1000L)
        val titledReminder = ReminderEntity(category = "Pago de luz", description = "Pagar antes de las 5", triggerAt = 1000L)

        assertEquals("Mi recordatorio", emptyReminder.displayTitle())
        assertEquals("Mi recordatorio", blankReminder.displayTitle())
        assertEquals("Pago de luz", titledReminder.displayTitle())
    }

    @Test
    fun emptyTitleAndDescriptionAreAllowedInReminderEntity() {
        val reminder = ReminderEntity(
            id = 0L,
            category = "",
            description = "",
            triggerAt = System.currentTimeMillis() + 60000L,
            repeat = ReminderRepeat.NONE
        )

        assertTrue(reminder.category.isEmpty())
        assertTrue(reminder.description.isEmpty())
        assertEquals("Mi recordatorio", reminder.displayTitle())
        assertFalse(reminder.completed)
    }
}
