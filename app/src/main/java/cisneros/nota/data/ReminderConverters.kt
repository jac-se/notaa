package cisneros.nota.data

import androidx.room.TypeConverter

class ReminderConverters {
    @TypeConverter
    fun fromRepeat(value: ReminderRepeat): String = value.name

    @TypeConverter
    fun toRepeat(value: String): ReminderRepeat =
        ReminderRepeat.entries.firstOrNull { it.name == value } ?: ReminderRepeat.NONE
}
