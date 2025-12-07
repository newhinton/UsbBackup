package de.felixnuesse.usbbackup.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.felixnuesse.usbbackup.R
import de.felixnuesse.usbbackup.broadcasts.BackupTaskBroadcastReciever
import de.felixnuesse.usbbackup.broadcasts.BackupTaskBroadcastReciever.Companion.UPDATE_ONGOING_TASK
import java.util.UUID

class NotificationMiddleware(private var mContext: Context, private var mProgressProvider: ProgressProvider): ProgressCallback {



    private var mNotifications = Notifications(mContext, 0)
    private var taskId = -1



    private var lastTitle: String? = null
    private var lastMessage: String? = null
    private var lastOngoing: Boolean? = null
    private var lastCancelable: Boolean? = null
    private var lastProgress: Int? = null

    private var lastProgressObject: Progress? = null
    private var lastProgressMessage: String? = null

    fun prepare(workerId: UUID, taskId: Int) {
        val id = (0..Integer.MAX_VALUE).random()
        mNotifications = Notifications(mContext, id)
        mNotifications.mUuid = workerId
        this.taskId = taskId
    }


    // todo: move
    private fun notifyMainActivity(title: String, message: String, progress: Int = -1) {
        val intent = Intent()
        intent.setAction(UPDATE_ONGOING_TASK)
        intent.putExtra("id", taskId)
        intent.putExtra("title", title)
        intent.putExtra("message", message)
        intent.putExtra("progress", progress)
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
    }


    override fun onProgressed(message: String) {
        val progress = mProgressProvider.getProgress()

        if(progress == lastProgressObject && message == lastProgressMessage) {
            Log.e("NotificationMiddleware", "Progress: Same, skip.")
            return
        }

        Log.e("NotificationMiddleware", "Backing up ${progress.currentSource}... || $message || ${progress.getProgress()}")
        log(mContext.getString(R.string.backup_notification_ongoing_title, progress.currentSource), message, true, false, progress.getProgress())
        lastProgressObject = progress.copy()
        lastProgressMessage = message
    }

    fun error(title: String, message: String, overrideId: Int = 0) {
        Log.e("NotificationMiddleware", "Err: $title || $message}")
        mNotifications.showError(title, message, overrideId)
        endTask()
    }

    fun log(title: String, message: String, ongoing: Boolean = false, cancelable: Boolean = true, progress: Int = -1) {

        if(
            title == lastTitle &&
            message == lastMessage &&
            ongoing == lastOngoing &&
            cancelable == lastCancelable
            && progress == lastProgress
            ) {
            Log.e("NotificationMiddleware", "Log: Same, skip.")
            return
        }


        Log.e("NotificationMiddleware", "Log: $title || $message}")
        mNotifications.showNotification(title, message, ongoing, cancelable, progress)
        notifyMainActivity(title, message, progress)
    }

    fun success(title: String, message: String) {
        Log.e("NotificationMiddleware", "Suc: $title || $message}")
        mNotifications.showNotificationSuccess(title, message)
        endTask()
    }
    fun endTask() {
        mNotifications.dismissDefaultNotification()
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(Intent(BackupTaskBroadcastReciever.EXIT_ONGOING_TASK))
    }


}