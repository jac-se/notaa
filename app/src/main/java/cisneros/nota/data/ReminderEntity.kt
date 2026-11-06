package cisneros.nota.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderRepeat { NONE, DAILY, WEEKLY }

/** Un aviso definido por la persona usuaria, independiente de sus notas. */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String = "",
    val description: String = "",
    val triggerAt: Long,
    val repeat: ReminderRepeat = ReminderRepeat.NONE,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

fun ReminderEntity.displayTitle(): String = category.ifBlank { "Mi recordatorio" }

