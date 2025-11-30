package de.felixnuesse.usbbackup

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import de.felixnuesse.usbbackup.UriUtils.Companion.getUriMetadata
import de.felixnuesse.usbbackup.database.Source
import de.felixnuesse.usbbackup.databinding.RecyclerviewSourceBinding

class SourceListAdapter(private val sources: List<Source>, private val mContext: Context, private val callbacks: SourceItemCallback) : RecyclerView.Adapter<SourceListAdapter.Row>() {

    inner class Row(var binding: RecyclerviewSourceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setTask(source: Source) {
            binding.source.text = getUriMetadata(mContext, source.uri.toUri())

            binding.source.isChecked = source.encrypt
            updateSwitchIcon()

            binding.deleteButton.setOnClickListener {
                callbacks.delete(source.uri)
            }

            binding.source.setOnCheckedChangeListener { view, isChecked ->
                callbacks.setEncrypted(source.uri, isChecked)
                updateSwitchIcon()
            }
        }

        fun updateSwitchIcon() {
            if(binding.source.isChecked) {
                binding.source.setThumbIconResource(R.drawable.icon_lock)
            } else {
                binding.source.setThumbIconResource(R.drawable.icon_lock_open)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Row {
        val binding = RecyclerviewSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Row(binding)
    }

    override fun onBindViewHolder(row: Row, position: Int) {
        row.setTask(sources[position])
    }

    override fun getItemCount() = sources.size
}