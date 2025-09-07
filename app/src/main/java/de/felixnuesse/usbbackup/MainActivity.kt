package de.felixnuesse.usbbackup

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import de.felixnuesse.usbbackup.database.BackupTask
import de.felixnuesse.usbbackup.database.BackupTaskMiddleware
import de.felixnuesse.usbbackup.databinding.ActivityMainBinding
import de.felixnuesse.usbbackup.dialog.ConfirmDialog
import de.felixnuesse.usbbackup.dialog.DialogCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity(), PopupCallback, DialogCallbacks {


    private lateinit var binding: ActivityMainBinding
    private lateinit var mBackupTaskMiddleware: BackupTaskMiddleware
    private lateinit var mPreferences: Prefs


    private val pushNotificationPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
        // todo: handle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        mPreferences = Prefs(this)
        if (!mPreferences.showIntro("0.0.0")) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.main)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestNotificationPermissions()

        binding.addFab.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
        }

        binding.moreButton.setOnClickListener {
            getPopupMenu().show()
        }


        mBackupTaskMiddleware = BackupTaskMiddleware.get(this)
        updateList()

        NotificationWorker.schedule(this)
    }

    override fun onResume() {
        super.onResume()
        updateList()
    }


    fun updateList() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val adapter = TaskListAdapter(mBackupTaskMiddleware.getAll(), this@MainActivity, this@MainActivity)

                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    binding.taskList.setLayoutManager(LinearLayoutManager(this@MainActivity))
                    binding.taskList.adapter = adapter
                }
            }
        }
    }

    override fun click(task: BackupTask, menuItemId: Int): Boolean {
        return when(menuItemId) {
            R.id.taskMenuItemCopy -> {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val newId = mBackupTaskMiddleware.copy(task.id!!)
                        AddActivity.startEdit(newId, this@MainActivity)
                    }
                }
                true
            }
            R.id.taskMenuItemEdit -> {
                AddActivity.startEdit(task.id!!, this@MainActivity)
                true
            }
            R.id.taskMenuItemDelete -> {
                ConfirmDialog(this@MainActivity, this@MainActivity).showDialog(task.id!!, task.name)
                true
            }
            R.id.taskMenuItemEnable -> {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        task.enabled = true
                        mBackupTaskMiddleware.update(task)
                        updateList()
                    }
                }
                true
            }
            R.id.taskMenuItemDisable -> {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        task.enabled = false
                        mBackupTaskMiddleware.update(task)
                        updateList()
                    }
                }
                true
            }

            else -> false
        }
    }

    override fun setText(text: String, id: Int) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val task = mBackupTaskMiddleware.get(id)
                task.containerPW = text
                mBackupTaskMiddleware.update(task)
                updateList()
            }
        }
    }

    override fun confirmDelete(id: Int) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                mBackupTaskMiddleware.deleteById(id)
                updateList()
            }
        }
    }

    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pushNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    fun getPopupMenu(): PopupMenu {
        val popupMenu = PopupMenu(this, binding.moreButton)
        popupMenu.menuInflater.inflate(R.menu.menu_main, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when(menuItem.itemId) {
                R.id.action_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }
        return popupMenu
    }
}