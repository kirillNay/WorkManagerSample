package nay.kirill.workmanagersample

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

class ReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.i("WorkManagerSample", "doWork")
        val isFormSubmitted = !File(applicationContext.filesDir, "draft_form.json").exists()

        if (!isFormSubmitted) {
            sendNotification()
        }

        return Result.success()
    }

    private fun sendNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(applicationContext, "reminder_channel")
            .setSmallIcon(android.R.drawable.alert_light_frame)
            .setContentTitle("Не забудьте подать заявление!")
            .setContentText("Вы начали заполнение формы, но не завершили. Вернитесь и отправьте заявление.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
