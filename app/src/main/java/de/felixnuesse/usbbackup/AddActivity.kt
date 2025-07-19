package de.felixnuesse.usbbackup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import de.felixnuesse.usbbackup.UriUtils.Companion.getStorageLabel
import de.felixnuesse.usbbackup.database.BackupTask
import de.felixnuesse.usbbackup.database.BackupTaskMiddleware
import de.felixnuesse.usbbackup.database.Source
import de.felixnuesse.usbbackup.databinding.ActivityAddBinding
import de.felixnuesse.usbbackup.extension.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AddActivity : AppCompatActivity(), SourceItemCallback {

    companion object {
        private const val SOURCE_REQUEST_ID = 423
        private const val TARGET_REQUEST_ID = 424
        private const val SOURCE_LAST_URI = "SOURCE_LAST_URI"
        private const val INTENT_EXTRA_ID = "INTENT_EXTRA_ID"

        fun startEdit(id: Int, context: Context) {
            val editIntent = Intent(context, AddActivity::class.java)
            editIntent.putExtra(INTENT_EXTRA_ID, id)
            context.startActivity(editIntent)
        }
    }

    private lateinit var binding: ActivityAddBinding
    private lateinit var mPrefs: Prefs
    private lateinit var mbackupMiddleware: BackupTaskMiddleware

    private var mSourceList = arrayListOf<Source>()
    private var mTargetUri: Uri? = null
    private var mExistingId: Int = -1


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
        mbackupMiddleware = BackupTaskMiddleware.get(this@AddActivity)

        prepareView(intent)


        binding.sourceUriFolderButton.setOnClickListener { pick(SOURCE_REQUEST_ID, mPrefs.getString(SOURCE_LAST_URI, null)) }
        binding.sourceUriFileButton.setOnClickListener { openDoc(SOURCE_REQUEST_ID) }
        binding.targetUriFolderButton.setOnClickListener { pick(TARGET_REQUEST_ID) }

        binding.nameTextfield.addTextChangedListener(getUpdateableTextWatcher())
        binding.pwTextfield.addTextChangedListener(getUpdateableTextWatcher())


        binding.saveFab.setOnClickListener {
            mTargetUri?.let { uri -> persist(uri) }
            mSourceList.forEach {
                persist(it.uri.toUri())
            }

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val backup = BackupTask.new(binding.nameTextfield.text.toString(), mSourceList, mTargetUri.toString())

                    if (binding.pwTextfield.text.toString().isNotBlank()) {
                        backup.containerPW = binding.pwTextfield.text.toString()
                    }

                    if(mExistingId == -1) {
                        mbackupMiddleware.insert(backup)
                    } else {
                        backup.id = mExistingId
                        mbackupMiddleware.update(backup)
                    }
                }
            }
            finish()
        }

    }

    private fun prepareView(intent: Intent) {
        mExistingId = intent.getIntExtra(INTENT_EXTRA_ID, -1)
        if(mExistingId == -1) return

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val existing = mbackupMiddleware.get(mExistingId)
                mSourceList = existing.sources
                runOnUiThread {
                    binding.nameTextfield.setText(existing.name)
                    mTargetUri = existing.targetUri.toUri()
                    binding.pwTextfield.setText(existing.containerPW)
                    updateUi()
                }
            }
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


        // states
        val hasSourceEntries = mSourceList.isNotEmpty()
        val hasEncryptedSourceEntries = mSourceList.filter { it.encrypt }.toList().isNotEmpty()
        val passwordSet = binding.pwTextfield.text.toString().isNotBlank()

        val hasName = binding.nameTextfield.text.toString().isNotBlank()
        val hasTarget = mTargetUri != null

        // visibility

        val baseConditions = hasSourceEntries && hasTarget && hasName
        // encrypted items and password, or no encrypted items are fine
        val encryptionConditions = (hasEncryptedSourceEntries && passwordSet) || !hasEncryptedSourceEntries
        binding.saveFab.isEnabled = baseConditions && encryptionConditions

        binding.pwInput.visible(hasEncryptedSourceEntries)
        binding.layoutNoPassword.visible(!passwordSet && hasEncryptedSourceEntries)

        // data
        if(hasTarget) {
            binding.targetUriMetadata.text = getStorageLabel(this, mTargetUri!!) + ": "+mTargetUri!!.path?.split(":")[1]
        }

        binding.sourceList.layoutManager = LinearLayoutManager(this)
        binding.sourceList.adapter = SourceListAdapter(mSourceList, this, this)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if(resultCode == RESULT_OK) {
            when(requestCode) {
                SOURCE_REQUEST_ID -> resultData?.data?.also {
                    uri -> mSourceList.add(Source(-1, uri.toString(), false))
                    mPrefs.setString("SOURCE_LAST_URI", uri.toString())
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

    override fun delete(uri: String) {
        mSourceList.removeIf { it.uri == uri }
        updateUi()
    }

    override fun setEncrypted(uri: String, encrypt: Boolean) {
        mSourceList.forEach {
            if(it.uri == uri) {
                it.encrypt = encrypt
            }
        }
        updateUi()
    }
}