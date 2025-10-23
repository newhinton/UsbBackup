package de.felixnuesse.usbbackup.mediascanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.storage.StorageManager
import android.util.Log
import de.felixnuesse.usbbackup.worker.Notifications

class MediaBroadcastReceiver: BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
        }

        Log.e("TAG", "Recieved USB-Media-Broadcast: ${intent.action} for device: ${device?.deviceId}${device?.deviceName}")

        // if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {}

        if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
            Notifications(context, 0).dismissSuccessNotification()
        }
    }
}