package de.felixnuesse.usbbackup.worker


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.felixnuesse.usbbackup.R
import de.felixnuesse.usbbackup.receiver.NotificationReceiver
import de.felixnuesse.usbbackup.receiver.NotificationReceiver.Companion.ACTION_STOP
import de.felixnuesse.usbbackup.receiver.NotificationReceiver.Companion.EXTRA_UUID
import java.util.UUID


class Notifications(private var mContext: Context, private var mId: Int) {


    val mNotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private val NOTIFICATION_CHANNEL_ID = "backup_worker_notifications"
        private val NOTIFICATION_CHANNEL_ERROR_ID = "backup_worker_error_notifications"
        private val NOTIFICATION_ID = 5691
        private val NOTIFICATION_ERROR_ID = 15691
    }

    var mUuid: UUID? = null

    fun showNotification(title: String, message: String, ongoing: Boolean = false, cancellable: Boolean = true, progress: Int = -1) {
        showNotification(mId, title, message, ongoing, cancellable, progress)
    }

    fun showNotification(overrideId: Int, title: String, message: String, ongoing: Boolean = false, cancellable: Boolean = true, progress: Int = -1) {

        NotificationManagerCompat.from(mContext).areNotificationsEnabled()

        createNotificationChannel()

        val mBuilder = NotificationCompat.Builder(mContext)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_security_key)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))


        if(ongoing) {
            mBuilder.setProgress(100, progress, progress == -1)
            mBuilder.setOngoing(true)

            if(cancellable) {
                mBuilder.addAction(R.drawable.icon_cancel, "Cancel", getStopIntent())
            } else {
                mBuilder.addAction(R.drawable.icon_cancel, "Cancel", null)
            }
        }
        mNotificationManager.notify(NOTIFICATION_ID+overrideId, mBuilder.build())
    }


    fun showError(title: String, message: String) {

        NotificationManagerCompat.from(mContext).areNotificationsEnabled()

        createErrorNotificationChannel()

        val mBuilder = NotificationCompat.Builder(mContext)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_close)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        mNotificationManager.notify(NOTIFICATION_ERROR_ID+mId, mBuilder.build())
    }


    private fun getStopIntent(): PendingIntent {
        val stopIntent = Intent(mContext, NotificationReceiver::class.java)
        stopIntent.setAction(ACTION_STOP)
        stopIntent.putExtra(EXTRA_UUID, mUuid.toString())
        return PendingIntent.getBroadcast(mContext, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
    }


    fun dismiss() {
        mNotificationManager.cancel(NOTIFICATION_ID+mId)
    }


    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Backup Worker Channel",
            NotificationManager.IMPORTANCE_LOW
        )

        channel.setSound(null, null)
        mNotificationManager.createNotificationChannel(channel)
    }

    private fun createErrorNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ERROR_ID,
            "Backup Worker Error Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        mNotificationManager.createNotificationChannel(channel)
    }
}