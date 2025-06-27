package com.ridwanelly.ingatin.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ridwanelly.ingatin.R

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Ambil data yang kita kirim dari AddTugasActivity
        val taskName = inputData.getString("TASK_NAME") ?: "Ada tugas baru!"
        val taskDescription = inputData.getString("TASK_DESCRIPTION") ?: "Jangan lupa dikerjakan ya!"

        // Panggil fungsi untuk menampilkan notifikasi
        showNotification(taskName, taskDescription)

        // Beritahu WorkManager bahwa tugas berhasil
        return Result.success()
    }

    private fun showNotification(title: String, description: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ingatin_notification_channel"
        val channelName = "Ingatin Notifications"

        // Untuk Android Oreo (API 26) ke atas, kita WAJIB membuat channel notifikasi
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_stat_notification) // Buat icon notifikasi baru
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Tampilkan notifikasi dengan ID unik
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}