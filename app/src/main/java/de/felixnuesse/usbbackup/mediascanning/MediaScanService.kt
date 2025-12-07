package de.felixnuesse.usbbackup.mediascanning

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import de.felixnuesse.usbbackup.broadcasts.MediaBroadcastReceiver
import de.felixnuesse.usbbackup.broadcasts.MediaBroadcastReceiver.Companion.NEW_VOLUME_BROADCAST
import de.felixnuesse.usbbackup.worker.BackupWorker
import de.felixnuesse.usbbackup.worker.Notifications

class MediaScanService : Service() {

    companion object {

        fun informAboutNewVolume(context: Context) {
            Log.e("MediaScanService", "Inform MediaBroadcastReciever about new volume!")
            val intent = Intent(NEW_VOLUME_BROADCAST).apply {
                setPackage("de.felixnuesse.usbbackup.broadcasts")
                setClass(context, MediaBroadcastReceiver::class.java)
            }

            context.sendBroadcast(intent)
        }

    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForegroundService()
        val context = this.applicationContext
        Thread {
            scan(context)
            stopSelf()
        }.start()

        return START_NOT_STICKY
    }

    private fun scan(context: Context) {
        val scanner = Scanner(context)

        // Simulate background work
        for (i in 1..5) {
            Log.e("MediaScanService", "Waiting for new drive to be mounted…")
            val newDrive = scanner.getNewDrive()
            if(newDrive != null) {
                Log.e("MediaScanService", "Found new volume: ${newDrive.uuid.toString()}")
                informAboutNewVolume(context)
                BackupWorker.now(context, newDrive.uuid.toString())
                return
            } else {
                Thread.sleep(1000)
            }
        }
    }

    private fun startAsForegroundService() {
        // create the notification channel
        val notification = Notifications(applicationContext, 0).getForegroundScanNotification().build()

        ServiceCompat.startForeground(
            this,
            1,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            } else {
                0
            }
        )
    }


}
