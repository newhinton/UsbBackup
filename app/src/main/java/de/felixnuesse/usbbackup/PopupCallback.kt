package de.felixnuesse.usbbackup

import de.felixnuesse.usbbackup.database.BackupTask

interface PopupCallback {

    fun click(id: BackupTask, menuItemId: Int): Boolean

}