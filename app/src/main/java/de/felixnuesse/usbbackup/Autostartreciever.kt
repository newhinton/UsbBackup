package de.felixnuesse.linkreciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.felixnuesse.usbbackup.worker.NotificationWorker

class Autostartreciever : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals("android.intent.action.BOOT_COMPLETED")) {
            NotificationWorker.schedule(context)
        }
    }

}
