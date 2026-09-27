package de.felixnuesse.usbbackup.mediascanning

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.felixnuesse.usbbackup.database.BackupTaskMiddleware
import de.felixnuesse.usbbackup.worker.BackupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class NotificationReceiver: BroadcastReceiver() {

    companion object {
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_POSTPONE = "ACTION_POSTPONE"
        const val EXTRA_UUID = "EXTRA_UUID"
        const val EXTRA_ID = "EXTRA_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("NotificationReceiver", "Recieved Notification Intent with action: ${intent.action}")
        if(intent.action == ACTION_STOP) {
            BackupWorker.stop(context, UUID.fromString(intent.getStringExtra(EXTRA_UUID)))
        }

        if(intent.action == ACTION_POSTPONE) {
            val days14 = 14 * 24 * 60 * 60 * 1000L
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            CoroutineScope(Dispatchers.IO).launch {
                val db = BackupTaskMiddleware.get(context)
                val intentTaskId = intent.getIntExtra(EXTRA_ID, -1)
                val task = db.get(intentTaskId)
                task.lastSuccessfulBackup = task.lastSuccessfulBackup?.plus(days14)
                db.update(task)

                val notificationToCancel = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                notificationManager.cancel(notificationToCancel)
            }
        }
    }
}