package de.felixnuesse.usbbackup.worker

interface StateCallback {
    fun wasStopped(): Boolean

    fun onProgressed(message: String)
}