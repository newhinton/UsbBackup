package de.felixnuesse.usbbackup.mediascanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.felixnuesse.usbbackup.MainActivity
import de.felixnuesse.usbbackup.worker.Notifications


class MediaBroadcastReceiver(private var callback: MediaBroadcastRecieverCallback? = null): BroadcastReceiver() {

    companion object {
        const val NEW_VOLUME_BROADCAST = "de.felixnuesse.usbbackup.NEW_VOLUME"

        fun getFilter(): IntentFilter {
            val filter = IntentFilter()
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            filter.addAction(NEW_VOLUME_BROADCAST)
            return filter
        }

        fun informAboutNewVolume(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            intent.action = NEW_VOLUME_BROADCAST
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            context.startActivity(intent)
        }
    }


    enum class STATE {
        CONNECTED,
        DISCONNECTED,
        UNKNOWN;

        fun isAttached(): Boolean {
            return this == CONNECTED
        }

        fun isDetached(): Boolean {
            return this == DISCONNECTED
        }
    }

    constructor(): this(null)


    override fun onReceive(context: Context, intent: Intent) {

        val device = getDevice(intent)
        val state = getState(intent)

        Log.e("TAG", "Recieved USB-Media-Broadcast: ${intent.action} for device: ${device?.deviceId}${device?.deviceName}")

        if (state.isDetached()) {
            Notifications(context, 0).dismissSuccessNotification()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (state.isDetached()) {
                this.callback?.onDisconnected()
            }
        }, 750)
    }

    fun getDevice(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
        }
    }

    fun getState(intent: Intent): STATE {
        val action = intent.action
        val isDetached = action == UsbManager.ACTION_USB_DEVICE_DETACHED
        val isAttached = action == UsbManager.ACTION_USB_DEVICE_ATTACHED

        return if (isDetached) {
            STATE.DISCONNECTED
        } else if (isAttached) {
            STATE.CONNECTED
        } else {
            STATE.UNKNOWN
        }
    }
}