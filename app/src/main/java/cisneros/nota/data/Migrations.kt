// app/src/main/java/cisneros/nota/data/Migrations.kt
package cisneros.nota.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // Leer columnas reales de la tabla vieja
        val cols = mutableSetOf<String>()
        val c = db.query("PRAGMA table_info(notes)")
        while (c.moveToNext()) {
            cols += c.getString(c.getColumnIndexOrThrow("name"))
        }
        c.close()

        val hasContent = "content" in cols
        val hasBody = "body" in cols
        val deletedOldName = when {
            "deletedAt" in cols -> "deletedAt"
            "deleted_at" in cols -> "deleted_at"
            else -> null
        }

        // Si no existe "content" pero sí "body", es un rename => reconstruimos
        if (!hasContent && hasBody) {
            db.execSQL("""
                CREATE TABLE notes_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    deletedAt INTEGER
                )
            """.trimIndent())

            val delSelect = deletedOldName ?: "NULL"

            db.execSQL("""
                INSERT INTO notes_new (id, title, content, createdAt, deletedAt)
                SELECT id, title, body, createdAt, $delSelect
                FROM notes
            """.trimIndent())

            db.execSQL("DROP TABLE notes")
            db.execSQL("ALTER TABLE notes_new RENAME TO notes")
        }
        // Si ya existe content, solo aseguramos deletedAt
        else {
            if (deletedOldName == null) {
                db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER")
            } else if (deletedOldName != "deletedAt") {
                // Normalizar nombre de deleted_at -> deletedAt
                db.execSQL("""
                    CREATE TABLE notes_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        deletedAt INTEGER
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO notes_new (id, title, content, createdAt, deletedAt)
                    SELECT id, title, content, createdAt, $deletedOldName
                    FROM notes
                """.trimIndent())

                db.execSQL("DROP TABLE notes")
                db.execSQL("ALTER TABLE notes_new RENAME TO notes")
            }
        }
    }
}
