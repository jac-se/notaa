package cisneros.nota.data

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val notes: List<NoteEntity>,
    val reminders: List<ReminderEntity>,
)

/** Respaldo local. Lee tanto el formato histórico (un arreglo de notas) como el nuevo. */
object JsonBackup {
    fun toJson(notes: List<NoteEntity>, reminders: List<ReminderEntity>): String {
        val root = JSONObject()
        root.put("formatVersion", 2)
        root.put("notes", JSONArray(notes.map(::noteToJson)))
        root.put("reminders", JSONArray(reminders.map(::reminderToJson)))
        return root.toString(2)
    }

    fun fromJson(json: String): BackupData {
        if (json.isBlank()) return BackupData(emptyList(), emptyList())
        val trimmed = json.trim()
        if (trimmed.startsWith("[")) {
            return BackupData(notesFromArray(JSONArray(trimmed)), emptyList())
        }
        val root = JSONObject(trimmed)
        return BackupData(
            notes = notesFromArray(root.optJSONArray("notes") ?: JSONArray()),
            reminders = remindersFromArray(root.optJSONArray("reminders") ?: JSONArray()),
        )
    }

    fun readFromUri(cr: ContentResolver, uri: Uri): BackupData {
        val text = cr.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }.orEmpty()
        return fromJson(text)
    }

    private fun noteToJson(note: NoteEntity) = JSONObject().apply {
        put("id", note.id)
        put("title", note.title)
        put("body", note.body)
        put("createdAt", note.createdAt)
        put("deletedAt", note.deletedAt ?: JSONObject.NULL)
        put("reminderAt", note.reminderAt ?: JSONObject.NULL)
    }

    private fun reminderToJson(reminder: ReminderEntity) = JSONObject().apply {
        put("id", reminder.id)
        put("category", reminder.category)
        put("description", reminder.description)
        put("triggerAt", reminder.triggerAt)
        put("repeat", reminder.repeat.name)
        put("completed", reminder.completed)
        put("createdAt", reminder.createdAt)
    }

    private fun notesFromArray(arr: JSONArray): List<NoteEntity> = buildList {
        for (index in 0 until arr.length()) {
            val item = arr.getJSONObject(index)
            add(NoteEntity(
                id = item.optLong("id", 0),
                title = item.optString("title", ""),
                body = item.optString("body", ""),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                deletedAt = if (item.isNull("deletedAt")) null else item.optLong("deletedAt"),
                reminderAt = if (item.isNull("reminderAt")) null else item.optLong("reminderAt"),
            ))
        }
    }

    private fun remindersFromArray(arr: JSONArray): List<ReminderEntity> = buildList {
        for (index in 0 until arr.length()) {
            val item = arr.getJSONObject(index)
            add(ReminderEntity(
                id = item.optLong("id", 0),
                category = item.optString("category", ""),
                description = item.optString("description", ""),
                triggerAt = item.optLong("triggerAt", System.currentTimeMillis()),
                repeat = ReminderRepeat.entries.firstOrNull {
                    it.name == item.optString("repeat", "NONE")
                } ?: ReminderRepeat.NONE,
                completed = item.optBoolean("completed", false),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
            ))
        }
    }
}
