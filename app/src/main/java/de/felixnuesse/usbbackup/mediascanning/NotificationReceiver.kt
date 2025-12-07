package de.felixnuesse.usbbackup.mediascanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.felixnuesse.usbbackup.worker.BackupWorker
import java.util.UUID

class NotificationReceiver: BroadcastReceiver() {

    companion object {
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_UUID = "EXTRA_UUID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("NotificationReceiver", "Recieved Notification Intent with action: ${intent.action}")
        if(intent.action == ACTION_STOP) {
            BackupWorker.stop(context, UUID.fromString(intent.getStringExtra(EXTRA_UUID)))
        }
    }
}