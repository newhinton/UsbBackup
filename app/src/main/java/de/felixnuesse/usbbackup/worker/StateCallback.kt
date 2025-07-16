package de.felixnuesse.usbbackup.worker

interface StateCallback {

    fun onProgressed()

    fun wasStopped(): Boolean

}