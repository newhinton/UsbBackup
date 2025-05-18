package de.felixnuesse.usbbackup.dialog

interface DialogCallbacks {

    fun setText(text: String, id: Int)

    fun confirmDelete(id: Int)
}