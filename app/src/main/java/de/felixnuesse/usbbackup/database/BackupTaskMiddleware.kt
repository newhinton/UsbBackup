package de.felixnuesse.usbbackup.database

import android.content.Context
import android.util.Log

class BackupTaskMiddleware(private var backupDao: BackupTaskDao, private var sourceDao: SourceDao) {

    companion object {
        fun get(context: Context): BackupTaskMiddleware {
            val db = AppDatabase.getDatabase(context)
            return BackupTaskMiddleware(db.backupDao(), db.sourceDao())
        }
    }

    fun getAll(): List<BackupTask> {
        val all = backupDao.getAll()
        all.forEach {
            loadSources(it)
        }
        return all
    }

    fun get(id: Int): BackupTask {
        val res = backupDao.get(id)
        loadSources(res)
        return res
    }

    fun insertAll(vararg entry: BackupTask) {
        entry.forEach(::insert)
    }

    fun copy(id: Int): Int {
        val entry = get(id)
        entry.id = null
        entry.name += " (Copy)"
        entry.sources.forEach {
            it.parentId = -1
        }
        return insert(entry)
    }

    fun insert(entry: BackupTask): Int {
        val id = entry.id ?: backupDao.insert(entry).toInt()
        storeSources(entry.sources, id)
        return id
    }

    fun update(entry: BackupTask) {
        backupDao.update(entry)
        updateSources(entry.sources, entry.id!!)
    }

    fun delete(entry: BackupTask) {
        backupDao.delete(entry)
        entry.id?.let { sourceDao.deleteByParentId(it) }
    }

    fun deleteById(id: Int) {
        backupDao.deleteById(id)
        sourceDao.deleteByParentId(id)
    }

    fun updateSuccessTimestamp(id: Int) {
        val entry = get(id)
        entry.lastSuccessfulBackup = System.currentTimeMillis()
        update(entry)
    }

    private fun loadSources(task: BackupTask) {
        if(task.id != null) {
            task.sources = ArrayList(sourceDao.getByParent(task.id!!))
        } else {
            Log.w("BackupTaskMiddleware", "The task: '${task.name}' does not have an id!")
        }
    }

    private fun storeSources(sources: ArrayList<Source>, parentId: Int) {
        sources.forEach {
            it.parentId = parentId
            sourceDao.insert(it)
        }
    }

    private fun updateSources(sources: ArrayList<Source>, parentId: Int) {
        sourceDao.deleteByParentId(parentId)
        storeSources(sources, parentId)
    }
}