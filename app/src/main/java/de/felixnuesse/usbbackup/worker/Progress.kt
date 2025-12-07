package de.felixnuesse.usbbackup.worker

import android.util.Log

class Progress() {
    var overallSize = 0L
    var calculatedSize = 0L
    var currentSource = ""
    var currentPath = ""

    fun getProgress(): Int {
        val progress = (100L * calculatedSize) / overallSize
        //Log.e("Progress", "progress: ${progress.toInt()} $calculatedSize $overallSize")
        return progress.toInt()
    }

    fun copy(): Progress {
        val copy = Progress()
        copy.overallSize = this.overallSize
        copy.calculatedSize = this.calculatedSize
        copy.currentSource = this.currentSource
        copy.currentPath = this.currentPath
        return copy
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Progress

        if (overallSize != other.overallSize) return false
        if (calculatedSize != other.calculatedSize) return false
        if (currentSource != other.currentSource) return false
        if (currentPath != other.currentPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = overallSize.hashCode()
        result = 31 * result + calculatedSize.hashCode()
        result = 31 * result + currentSource.hashCode()
        result = 31 * result + currentPath.hashCode()
        return result
    }


}