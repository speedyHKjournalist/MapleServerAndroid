import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.server.Server

class StopServerWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val server = Server.getInstance()
            server.shutdown(false)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success()
        }
    }
}