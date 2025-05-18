package de.felixnuesse.usbbackup.worker


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.felixnuesse.usbbackup.R


class Notifications(private var context: Context) {


    val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private val NOTIFICATION_CHANNEL_ID = "backup_worker_notifications"
        private val NOTIFICATION_ID = 5691
    }


    fun showNotification(title: String, message: String) {

        NotificationManagerCompat.from(context).areNotificationsEnabled()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val mBuilder = NotificationCompat.Builder(context)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.security_key_24px)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build())
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Backup Worker Channel",
            NotificationManager.IMPORTANCE_LOW
        )

        channel.setSound(null, null)
        mNotificationManager.createNotificationChannel(channel)
    }
}