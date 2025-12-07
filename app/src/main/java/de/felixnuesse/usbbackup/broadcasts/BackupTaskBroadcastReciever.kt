package de.felixnuesse.usbbackup.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class BackupTaskBroadcastReciever(private var callback: (Intent) -> Unit) : BroadcastReceiver() {

    companion object {
        const val UPDATE_ONGOING_TASK = "de.felixnuesse.usbbackup.UPDATE_ONGOING_TASK"
        const val EXIT_ONGOING_TASK = "de.felixnuesse.usbbackup.EXIT_ONGOING_TASK"

        fun getFilter(): IntentFilter {
            val filter = IntentFilter()
            filter.addAction(UPDATE_ONGOING_TASK)
            filter.addAction(EXIT_ONGOING_TASK)
            return filter
        }
    }


    override fun onReceive(context: Context, intent: Intent) {
        callback.invoke(intent)
    }
}