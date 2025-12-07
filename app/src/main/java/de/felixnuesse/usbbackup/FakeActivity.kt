package de.felixnuesse.usbbackup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import de.felixnuesse.usbbackup.mediascanning.MediaScanService

class FakeActivity: AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = ""
        setContentView(R.layout.loading_activity)

        Log.e("FakeActivity", "Start MediaScanService and exit")
        startForegroundService(Intent(this, MediaScanService::class.java))
        finish()
    }

}