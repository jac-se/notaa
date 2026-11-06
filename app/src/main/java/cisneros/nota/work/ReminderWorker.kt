package cisneros.nota.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cisneros.nota.data.AppDb
import cisneros.nota.data.ReminderRepeat
import cisneros.nota.data.displayTitle

class ReminderWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(REMINDER_ID, -1L)
        if (id < 0) return Result.failure()
        val dao = AppDb.get(applicationContext).reminderDao()
        val reminder = dao.getById(id) ?: return Result.success()
        if (reminder.completed || reminder.triggerAt > System.currentTimeMillis() + 60_000L) return Result.success()

        ReminderNotificationHelper.showNotification(
            context = applicationContext,
            notificationId = reminder.id.toInt(),
            title = reminder.displayTitle(),
            description = reminder.description
        )

        if (reminder.repeat == ReminderRepeat.NONE) {
            dao.setCompleted(id, true)
        } else {
            val next = ReminderScheduleCalculator.nextTrigger(reminder.triggerAt, reminder.repeat)
            dao.updateTriggerAt(id, next)
            ReminderScheduler.schedule(applicationContext, reminder.copy(triggerAt = next))
        }
        return Result.success()
    }

    companion object {
        const val REMINDER_ID = "reminder_id"
    }
}
