package de.felixnuesse.usbbackup.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.widget.EditText

class InputDialog(private var mContext: Context, private var mCallback: DialogCallbacks) {

    fun showDialog(id: Int) {
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle("Set a Password for this task:")

        val input = EditText(mContext)

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
        builder.setView(input)


        // Set up the buttons
        builder.setPositiveButton("Set Password", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                mCallback.setText(input.getText().toString(), id)
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