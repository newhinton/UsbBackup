package de.felixnuesse.usbbackup.mediascanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.storage.StorageManager
import android.util.Log

class MediaBroadcastReceiver: BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        Log.e("TAG", "Recieved Broadcast!")

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val preMountVolumes = storageManager.storageVolumes



        if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
            }
            //Log.e("TAG", "Device: ${device.toString()}")

            Log.e("TAG", "It was a media broadcast: ${intent.action}")
            Scanner(context).tryFindingNewVolume(1000, storageManager, preMountVolumes, 5)
        }

        if (intent.action!!.matches(Regex("MEDIA_[a-zA-Z]+"))) {
            Log.e("TAG", "It was a media broadcast: ${intent.action}")
        }
    }
}