package de.felixnuesse.usbbackup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import de.felixnuesse.usbbackup.UriUtils.Companion.getStorageId
import de.felixnuesse.usbbackup.database.AppDatabase
import de.felixnuesse.usbbackup.database.BackupTask
import de.felixnuesse.usbbackup.databinding.ActivityAddBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AddActivity : AppCompatActivity() {


    companion object {
        private const val SOURCE_REQUEST_ID = 423
        private const val TARGET_REQUEST_ID = 424
    }

    private lateinit var binding: ActivityAddBinding

    private var mSourceUri: Uri? = null
    private var mTargetUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.main)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.sourceUriFolderButton.setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SOURCE_REQUEST_ID) }
        binding.targetUriFolderButton.setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), TARGET_REQUEST_ID) }
        binding.sourceUriFileButton.setOnClickListener { openDoc(SOURCE_REQUEST_ID) }


        binding.saveFab.setOnClickListener {
            mTargetUri?.let { uri -> persist(uri) }
            mSourceUri?.let { uri -> persist(uri) }

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.Companion.getDatabase(this@AddActivity)
                    var backup = BackupTask.new("NameOfTask", mSourceUri.toString(), mTargetUri.toString())
                    db.backupDao().insert(backup)
                }
            }
            finish()
        }

    }

    private fun openDoc(id: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("*/*")
        startActivityForResult(intent, id)
    }


    private fun updateUi() {
        binding.sourceUri.text = mSourceUri.toString()
        setUriMetadata(binding.sourceUriMetadata, mSourceUri)

        binding.targetUri.text = mTargetUri.toString()
        setUriMetadata(binding.targetUriMetadata, mTargetUri)
    }

    private fun setUriMetadata(view: TextView, uri: Uri?) {
        if(uri != null) {
            var id = getStorageId(uri)
            try {
                val folder = DocumentFile.fromTreeUri(this, uri)

                view.text = "$id: ${folder?.name}"
            } catch (e: Exception) {
                val file = DocumentFile.fromSingleUri(this, uri)
                view.text = "$id: ${file?.name}"
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if(resultCode == RESULT_OK) {
            when(requestCode) {
                SOURCE_REQUEST_ID -> resultData?.data?.also { uri -> mSourceUri = uri }
                TARGET_REQUEST_ID -> resultData?.data?.also { uri -> mTargetUri = uri }
            }
            updateUi()
        }
    }

    fun persist(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)
    }
}