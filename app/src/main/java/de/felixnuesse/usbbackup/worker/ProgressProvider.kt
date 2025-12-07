package de.felixnuesse.usbbackup.worker

interface ProgressProvider {

    fun getProgress(): Progress
}