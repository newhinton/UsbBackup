package de.felixnuesse.usbbackup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import de.felixnuesse.usbbackup.database.BackupTask
import de.felixnuesse.usbbackup.databinding.RecyclerviewTaskBinding
import de.felixnuesse.usbbackup.worker.BackupWorker


class TaskListAdapter(private val tasks: List<BackupTask>, private val mContext: Context, private val mPopupCallback: PopupCallback) : RecyclerView.Adapter<TaskListAdapter.Row>() {

    inner class Row(var binding: RecyclerviewTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setTask(task: BackupTask) {
            binding.title.text = task.name

            var sourceUri = task.sourceUri.toUri()
            var targetUri = task.targetUri.toUri()
            var targetUriName = UriUtils.getStorageId(targetUri)

            binding.source.text = "${UriUtils.getStorageId(sourceUri)}: ${UriUtils.getName(mContext, sourceUri)}"
            binding.target.text = targetUriName

            var menu = getPopupMenu(binding.moreButton, task)

            binding.moreButton.setOnClickListener {
                menu.show()
            }


            if(!task.containerPW.isNullOrBlank()) {
                binding.layoutNoPassword.visibility = View.GONE
            } else {
                menu.menu.findItem(R.id.taskMenuItemDeletePassword).isVisible = false
            }


            if(StorageUtils.get(mContext, task.targetUri.toUri()) == null && targetUriName != "primary") {
                binding.layoutDiskAvailable.visibility = View.GONE
            }

            binding.startBackup.setOnClickListener {
                BackupWorker.now(mContext, task.id!!)
            }

        }
    }

    fun getPopupMenu(anchor: View, task: BackupTask): PopupMenu {
        val popupMenu = PopupMenu(mContext, anchor)
        popupMenu.menuInflater.inflate(R.menu.task_menu, popupMenu.menu)

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