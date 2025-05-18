package de.felixnuesse.usbbackup.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.widget.EditText

class ConfirmDialog(private var mContext: Context, private var mCallback: DialogCallbacks) {

    fun showDialog(id: Int, title: String) {
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle("Delete $title")
        builder.setMessage("Do you really want to delete this task?")
        builder .setIcon(android.R.drawable.ic_dialog_alert)

        // Set up the buttons
        builder.setPositiveButton("Delete!", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                mCallback.confirmDelete(id)
            }
        })
        builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.cancel()
            }
        })

        builder.show()
    }

}