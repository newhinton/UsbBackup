package de.felixnuesse.usbbackup

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import de.felixnuesse.usbbackup.databinding.ActivityAboutBinding
import java.util.Calendar
import androidx.core.net.toUri


class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.title = ""
        setSupportActionBar(binding.toolbar)

        binding.version.text = BuildConfig.VERSION_NAME

        binding.help.setOnClickListener {
            //openURL("")
        }


        binding.donate.setOnClickListener {
            openURL("https://felixnuesse.de/donate")
        }

        binding.copyright.setOnClickListener {
            //openURL("")
        }

        binding.copyright.text = this.getString(R.string.copyright, Calendar.getInstance().get(Calendar.YEAR).toString())
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_about, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_exit -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun openURL(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

}