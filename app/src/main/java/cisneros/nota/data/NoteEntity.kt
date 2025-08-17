package cisneros.nota.data // Paquete de datos

import androidx.room.ColumnInfo // Anotación para renombrar columnas
import androidx.room.Entity     // Marca esta clase como tabla de Room
import androidx.room.PrimaryKey // Define la clave primaria

/**
 * Representa una fila de la tabla "notes" en la base de datos.
 *
 * @property id       Identificador único (autogenerado).
 * @property title    Título de la nota (también lo usamos como fecha por defecto).
 * @property body     Contenido de la nota (texto largo).
 * @property createdAt Epoch millis de creación (para ordenar/mostrar fecha).
 * @property deletedAt Epoch millis de borrado si está en papelera; null si activa.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,    // PK autoincremental
    val title: String,                                    // Título de la nota
    @ColumnInfo(name = "content") val body: String,       // Renombrada a 'content' en BD
    val createdAt: Long = System.currentTimeMillis(),     // Marca de tiempo de creación
    @ColumnInfo(name = "deletedAt") val deletedAt: Long? = null // null => activa; no null => papelera
)
