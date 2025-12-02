package de.felixnuesse.usbbackup.mediascanning

import android.hardware.usb.UsbDevice

interface MediaBroadcastRecieverCallback {
    fun onDisconnected()
}
