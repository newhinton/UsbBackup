package de.felixnuesse.usbbackup.worker

import java.util.UUID

interface StateProvider {
    fun wasStopped(): Boolean
    fun workerId(): UUID
}