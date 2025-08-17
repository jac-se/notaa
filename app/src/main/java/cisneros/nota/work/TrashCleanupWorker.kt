package cisneros.nota.work // Paquete de trabajos

import android.content.Context                            // Contexto de ejecución
import androidx.work.CoroutineWorker                      // Worker basado en corrutinas
import androidx.work.WorkerParameters                     // Parámetros del worker
import cisneros.nota.data.AppDb                           // Para acceder a la BD
import kotlinx.coroutines.Dispatchers                     // Dispatcher IO
import kotlinx.coroutines.withContext                     // Cambiar dispatcher
import java.util.concurrent.TimeUnit                      // Días a millis

/**
 * Worker que elimina definitivamente notas en papelera con antigüedad > 30 días.
 * Se programa desde App.kt para correr una vez al día.
 */
class TrashCleanupWorker(
    appContext: Context,              // Provisto por WorkManager
    params: WorkerParameters          // Config de la ejecución
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = AppDb.get(applicationContext)                // Obtiene la BD
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30) // 30 días atrás
            db.noteDao().deleteTrashOlderThan(cutoff)             // Ejecuta limpieza
            Result.success()                                      // Reporta éxito
        } catch (e: Exception) {
            Result.retry()                                        // Reintentar si falló
        }
    }
}
