package de.felixnuesse.usbbackup.worker


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


class Notifications(private var context: Context) {


    val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private val NOTIFICATION_CHANNEL_ID = "async_archive_notification_channel"
        private val NOTIFICATION_ID = 5691
    }


    fun notifyPendingArchivalDone(title: String) {

        NotificationManagerCompat.from(context).areNotificationsEnabled()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val mBuilder = NotificationCompat.Builder(context)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setContentText("We archived: $title")
            .setAutoCancel(true)

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build())
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "test",
            NotificationManager.IMPORTANCE_LOW
        )

        channel.setSound(null, null)
        mNotificationManager.createNotificationChannel(channel)
    }
}