package cisneros.nota.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY completed ASC, triggerAt ASC")
    fun streamAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE completed = 0")
    suspend fun getPending(): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("UPDATE reminders SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("UPDATE reminders SET triggerAt = :triggerAt WHERE id = :id")
    suspend fun updateTriggerAt(id: Long, triggerAt: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM reminders WHERE category = :category AND description = :description AND triggerAt = :triggerAt)")
    suspend fun exists(category: String, description: String, triggerAt: Long): Boolean

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
