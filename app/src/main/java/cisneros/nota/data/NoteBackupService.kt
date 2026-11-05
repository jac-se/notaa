package cisneros.nota.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/**
 * Portable, versioned backup of active and trashed notes.
 * Never trusts IDs from another installation when restoring.
 */
class NoteBackupService(private val db: AppDb) {
    suspend fun exportJson(): String {
        val notes = db.noteDao().allForBackup()
        val array = JSONArray()
        notes.forEach { note ->
            array.put(JSONObject().apply {
                put("title", note.title)
                put("body", note.body)
                put("createdAt", note.createdAt)
                put("deletedAt", note.deletedAt ?: JSONObject.NULL)
            })
        }
        return JSONObject().apply {
            put("format", "cisneros.nota.backup")
            put("version", 1)
            put("notes", array)
        }.toString(2)
    }

    suspend fun restoreJson(raw: String): Int {
        require(raw.length <= 20_000_000) { "El archivo es demasiado grande" }
        val root = JSONObject(raw)
        require(root.optString("format") == "cisneros.nota.backup" &&
            root.optInt("version") == 1) { "Formato de respaldo no compatible" }
        val array = root.getJSONArray("notes")
        require(array.length() <= 100_000) { "Demasiadas notas en el respaldo" }
        val imported = ArrayList<NoteEntity>(array.length())
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val title = item.getString("title")
            val body = item.getString("body")
            val createdAt = item.getLong("createdAt")
            val deletedAt = if (item.isNull("deletedAt")) null else item.getLong("deletedAt")
            imported.add(NoteEntity(title = title, body = body, createdAt = createdAt, deletedAt = deletedAt))
        }
        return db.withTransaction {
            val existing = db.noteDao().allForBackup().map {
                listOf(it.title, it.body, it.createdAt.toString(), it.deletedAt?.toString() ?: "")
            }.toHashSet()
            var added = 0
            for (note in imported) {
                val key = listOf(note.title, note.body, note.createdAt.toString(), note.deletedAt?.toString() ?: "")
                if (existing.add(key)) {
                    db.noteDao().insert(note)
                    added++
                }
            }
            added
        }
    }
}
