package de.felixnuesse.usbbackup

import android.content.Context
import android.graphics.Paint
import android.icu.text.RelativeDateTimeFormatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import de.felixnuesse.usbbackup.UriUtils.Companion.getStorageLabel
import de.felixnuesse.usbbackup.UriUtils.Companion.getUriMetadata
import de.felixnuesse.usbbackup.database.BackupTask
import de.felixnuesse.usbbackup.databinding.RecyclerviewTaskBinding
import de.felixnuesse.usbbackup.utils.DateFormatter
import de.felixnuesse.usbbackup.worker.BackupWorker
import java.time.Duration
import java.time.Instant


class TaskListAdapter(private val tasks: List<BackupTask>, private val mContext: Context, private val mPopupCallback: PopupCallback) : RecyclerView.Adapter<TaskListAdapter.Row>() {

    inner class Row(var binding: RecyclerviewTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setTask(task: BackupTask) {
            binding.title.text = task.name

            if(!task.enabled) {
                binding.title.paintFlags = binding.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }


            val targetUri = task.targetUri.toUri()
            val targetUriName = getStorageLabel(mContext, targetUri) + ": "+targetUri.path?.split(":")[1]

            binding.source.text = task.sources.joinToString("\n") { getUriMetadata(mContext, it.uri.toUri())}
            binding.target.text = targetUriName

            val menu = getPopupMenu(binding.moreButton, task)
            binding.moreButton.setOnClickListener {
                menu.show()
            }

            if(task.lastSuccessfulBackup != BackupTask.NEVER) {
                binding.lastSuccessfulRun.text = DateFormatter.relative(task.lastSuccessfulBackup).capitalize()
            }

            if(!task.containerPW.isNullOrBlank()) {
                binding.noPasswordTextView.visibility = View.GONE
                binding.noPasswordImageView.visibility = View.INVISIBLE
                binding.passwordImageView.visibility = View.VISIBLE
            } else {
                binding.noPasswordImageView.visibility = View.VISIBLE
                binding.passwordImageView.visibility = View.INVISIBLE
            }


            if(task.enabled) {
                menu.menu.findItem(R.id.taskMenuItemDisable).isVisible = true
                menu.menu.findItem(R.id.taskMenuItemEnable).isVisible = false
            } else {
                menu.menu.findItem(R.id.taskMenuItemDisable).isVisible = false
                menu.menu.findItem(R.id.taskMenuItemEnable).isVisible = true
            }


            if(StorageUtils.get(mContext, task.targetUri.toUri()) == null && targetUriName != "primary") {
                binding.layoutDiskAvailable.visibility = View.GONE
            }

            binding.startBackup.setOnClickListener {
                BackupWorker.now(mContext, task.id!!)
                Toast.makeText(mContext, "Start Task: ${task.name}", Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun getPopupMenu(anchor: View, task: BackupTask): PopupMenu {
        val popupMenu = PopupMenu(mContext, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_task, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            if(task.id != null) {
                mPopupCallback.click(task, menuItem.itemId)
                true
            } else {
                false
            }
        }
        return popupMenu
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Row {
        val binding = RecyclerviewTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Row(binding)
    }

    override fun onBindViewHolder(row: Row, position: Int) {
        row.setTask(tasks[position])
    }


    override fun getItemCount() = tasks.size

}