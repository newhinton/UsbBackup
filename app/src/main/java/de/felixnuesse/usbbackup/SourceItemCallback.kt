package de.felixnuesse.usbbackup

interface SourceItemCallback {

    fun delete(uri: String)
    fun encrypted(uri: String, isEncrypted: Boolean)

}