package de.felixnuesse.usbbackup.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.util.Log
import de.felixnuesse.usbbackup.worker.BackupWorker

class MediaBroadcastReceiver: BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        Log.e("TAG", "Recieved Broadcast!")


        var sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        var preMountVolumes = sm.storageVolumes



        if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
            }
            //Log.e("TAG", "Device: ${device.toString()}")

            Log.e("TAG", "It was a media broadcast: ${intent.action}")
            Handler(Looper.getMainLooper()).postDelayed({
                // this requires timing, and a lot of assumptions. MEDIA_MOUNT would truly be better
                var newDrive = sm.storageVolumes.filter { it !in preMountVolumes }.toList().first()
                BackupWorker.Companion.now(context, newDrive.uuid.toString())
            }, 5000)


        }

        if (intent.action!!.matches(Regex("MEDIA_[a-zA-Z]+"))) {
            Log.e("TAG", "It was a media broadcast: ${intent.action}")
        }
    }
}