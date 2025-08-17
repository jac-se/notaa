package cisneros.nota.data // Paquete de datos

import android.content.ContentResolver // Para leer URIs del SAF
import android.content.Context         // Contexto para archivos internos
import android.net.Uri                 // Uri del SAF
import org.json.JSONArray              // JSON array simple
import org.json.JSONObject             // JSON object simple

/**
 * Utilidades para exportar/importar notas en JSON.
 * Implementación mínima (org.json) para evitar dependencias extra.
 */
object JsonBackup {

    /** Convierte una lista de notas a un JSON (array) legible con indentación. */
    fun toJson(list: List<NoteEntity>): String {
        val arr = JSONArray()
        list.forEach { n ->
            val o = JSONObject()
            o.put("id", n.id)
            o.put("title", n.title)
            o.put("body", n.body)
            o.put("createdAt", n.createdAt)
            if (n.deletedAt != null) o.put("deletedAt", n.deletedAt) else o.put("deletedAt", JSONObject.NULL)
            arr.put(o)
        }
        return arr.toString(2) // Indentación de 2 espacios
    }

    /** Parsea JSON (array) a lista de notas, tolerando campos faltantes. */
    fun fromJson(json: String): List<NoteEntity> {
        val out = mutableListOf<NoteEntity>()
        if (json.isBlank()) return out
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += NoteEntity(
                id = o.optLong("id", 0),
                title = o.optString("title", ""),
                body = o.optString("body", ""),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
            )
        }
        return out
    }

    // ---------- Almacenamiento interno (fallback) ----------

    /** Escribe el json de todas las notas en almacenamiento interno privado. */
    fun writeInternal(context: Context, list: List<NoteEntity>) {
        context.openFileOutput("backup_internal.json", Context.MODE_PRIVATE).use {
            it.write(toJson(list).toByteArray(Charsets.UTF_8))
        }
    }

    /** Lee el json interno; si no existe, devuelve lista vacía. */
    fun readInternal(context: Context): List<NoteEntity> {
        return try {
            val s = context.openFileInput("backup_internal.json").use {
                String(it.readBytes(), Charsets.UTF_8)
            }
            fromJson(s)
        } catch (_: Exception) { emptyList() }
    }

    // ---------- Lectura desde SAF ----------

    /** Lee texto de un Uri (carpeta/archivo elegido por el usuario con SAF). */
    fun readTextFromUri(cr: ContentResolver, uri: Uri): String {
        return cr.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }.orEmpty()
    }

    /** Lee y parsea notas desde un Uri JSON. */
    fun readNotesFromUri(cr: ContentResolver, uri: Uri): List<NoteEntity> =
        fromJson(readTextFromUri(cr, uri))
}
