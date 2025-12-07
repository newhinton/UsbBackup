package de.felixnuesse.usbbackup.worker

interface ProgressCallback {
    fun onProgressed(message: String)
}