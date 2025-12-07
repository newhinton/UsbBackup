package de.felixnuesse.usbbackup.broadcasts

interface MediaBroadcastRecieverCallback {
    fun onDisconnected()
    fun onNewVolume()
}
