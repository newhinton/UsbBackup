package de.felixnuesse.usbbackup.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.felixnuesse.usbbackup.worker.Notifications
import java.util.UUID


class MediaBroadcastReceiver(): BroadcastReceiver() {

    companion object {
        private var MAIN_ACTIVITY_CALLBACK: MediaBroadcastRecieverCallback? = null

        fun setCallback(callback: MediaBroadcastRecieverCallback) {
            MAIN_ACTIVITY_CALLBACK = callback
        }

        fun clearCallback(callback: MediaBroadcastRecieverCallback) {
            if(callback == MAIN_ACTIVITY_CALLBACK) {
                MAIN_ACTIVITY_CALLBACK = null
            }
        }


        private var WORKER_TASK_CALLBACKS: HashMap<UUID, MediaBroadcastRecieverCallback> = hashMapOf()

        fun registerCallback(uuid: UUID, callback: MediaBroadcastRecieverCallback) {
            WORKER_TASK_CALLBACKS.put(uuid, callback)
        }

        fun clear(uuid: UUID) {
            WORKER_TASK_CALLBACKS.remove(uuid)
        }

        const val NEW_VOLUME_BROADCAST = "de.felixnuesse.usbbackup.NEW_VOLUME"
    }


    enum class STATE {
        CONNECTED,
        DISCONNECTED,
        NEW_VOLUME,
        UNKNOWN;

        fun isAttached(): Boolean {
            return this == CONNECTED
        }

        fun isDetached(): Boolean {
            return this == DISCONNECTED
        }

        fun isNewVolume(): Boolean {
            return this == NEW_VOLUME
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        val device = getDevice(intent)
        val state = getState(intent)

        Log.e("MediaBroadcastReceiver", "Recieved USB-Media-Broadcast: ${intent.action}")
        if(!(state.isNewVolume() || state == STATE.UNKNOWN)){
            Log.e("MediaBroadcastReceiver", "Broadcast was for device: ${device?.deviceId}:${device?.deviceName}")
        }

        if (state.isDetached()) {
            Notifications(context, 0).dismissSuccessNotification()

            Handler(Looper.getMainLooper()).postDelayed({
                MAIN_ACTIVITY_CALLBACK?.onDisconnected()
                WORKER_TASK_CALLBACKS.forEach {
                    it.value.onDisconnected()
                }
            }, 750)
        }

        if(state.isNewVolume()) {
            MAIN_ACTIVITY_CALLBACK?.onNewVolume()
        }

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
        val isNewVolume = action == NEW_VOLUME_BROADCAST

        return if (isDetached) {
            STATE.DISCONNECTED
        } else if (isAttached) {
            STATE.CONNECTED
        } else if (isNewVolume) {
            STATE.NEW_VOLUME
        } else {
            STATE.UNKNOWN
        }
    }
}