import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.server.Server

class StartServerWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val args = arrayOf("-Xmx2048m", "-Dwz-path=wz", "-Djava.net.preferIPv4Stack=true")
            Server.getInstance(applicationContext)
            Server.main(args)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success()
        }
    }
}