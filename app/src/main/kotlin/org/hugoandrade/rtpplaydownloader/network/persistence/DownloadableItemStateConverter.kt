package org.hugoandrade.rtpplaydownloader.network.persistence

import androidx.room.TypeConverter
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem

class DownloadableItemStateConverter {

    @TypeConverter
    fun toOrdinal(state: DownloadableItem.State?): Int {
        return state?.ordinal ?: -1
    }

    @TypeConverter
    fun fromOrdinal(stateOrdinal : Int?): DownloadableItem.State? {
        return if (stateOrdinal == null || stateOrdinal < 0 || stateOrdinal >= DownloadableItem.State.values().size) null
        else enumValues<DownloadableItem.State>()[stateOrdinal]
    }
}