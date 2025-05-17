package de.felixnuesse.usbbackup

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.felixnuesse.usbbackup.database.BackupTask
import de.felixnuesse.usbbackup.databinding.RecyclerviewTaskBinding

class TaskListAdapter(private val tasks: List<BackupTask>, private val mContext: Context) : RecyclerView.Adapter<TaskListAdapter.Row>() {

    inner class Row(var binding: RecyclerviewTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setTask(task: BackupTask) {
            binding.title.text = task.name
            binding.source.text = task.sourceUri
            binding.target.text = task.targetUri
        }
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