package de.felixnuesse.usbbackup

interface SourceItemCallback {

    fun delete(uri: String)
    fun setEncrypted(uri: String, encrypt: Boolean)

}