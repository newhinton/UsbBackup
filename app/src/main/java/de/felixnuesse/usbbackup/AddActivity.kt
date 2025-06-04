package de.felixnuesse.usbbackup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
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
        private const val SOURCE_LAST_URI = "SOURCE_LAST_URI"
    }

    private lateinit var binding: ActivityAddBinding
    private lateinit var mPrefs: Prefs

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

        mPrefs = Prefs(this)

        binding.sourceUriFolderButton.setOnClickListener { pick(SOURCE_REQUEST_ID, mPrefs.getString(SOURCE_LAST_URI, null)) }
        binding.sourceUriFileButton.setOnClickListener { openDoc(SOURCE_REQUEST_ID) }
        binding.targetUriFolderButton.setOnClickListener { pick(TARGET_REQUEST_ID) }

        binding.nameTextfield.addTextChangedListener(getUpdateableTextWatcher())
        binding.pwTextfield.addTextChangedListener(getUpdateableTextWatcher())


        binding.saveFab.setOnClickListener {
            mTargetUri?.let { uri -> persist(uri) }
            mSourceUri?.let { uri -> persist(uri) }

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.Companion.getDatabase(this@AddActivity)
                    var backup = BackupTask.new(binding.nameTextfield.text.toString(), mSourceUri.toString(), mTargetUri.toString())

                    if (binding.pwTextfield.text.toString().isNotBlank()) {
                        backup.containerPW = binding.pwTextfield.text.toString()
                    }

                    db.backupDao().insert(backup)
                }
            }
            finish()
        }

    }

    private fun pick(id: Int, initialUriString: String? = null) {
        var startIntent = StorageUtils.getInitialTreeIntent(this)
        if(initialUriString != null) {
            startIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startIntent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUriString.toUri())
        }
        startActivityForResult(startIntent, id)
    }

    private fun openDoc(id: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("*/*")
        startActivityForResult(intent, id)
    }

    private fun updateUi() {
        if (mSourceUri != null && mTargetUri != null && !binding.nameTextfield.text.toString().isBlank()) {
            binding.saveFab.visibility = View.VISIBLE
        } else {
            binding.saveFab.visibility = View.INVISIBLE
        }

        if(mSourceUri!=null) {
            binding.sourceUri.text = mSourceUri.toString()
            setUriMetadata(binding.sourceUriMetadata, mSourceUri)
        }

        if(mTargetUri!=null) {
            binding.targetUri.text = mTargetUri.toString()
            setUriMetadata(binding.targetUriMetadata, mTargetUri)
        }

        binding.layoutNoPassword.visibility = if (binding.pwTextfield.text.toString().isBlank()) {
            View.VISIBLE
        } else {
            View.GONE
        }
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
                SOURCE_REQUEST_ID -> resultData?.data?.also {
                    uri -> mSourceUri = uri
                    mPrefs.setString("SOURCE_LAST_URI", mSourceUri.toString())
                }
                TARGET_REQUEST_ID -> resultData?.data?.also {
                    uri -> mTargetUri = uri
                    mPrefs.setString("TARGET_LAST_URI", mTargetUri.toString())
                }
            }
            updateUi()
        }
    }

    fun persist(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    fun getUpdateableTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                updateUi()
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
    }
}