package org.hugoandrade.rtpplaydownloader.network.parsing

import android.databinding.ObservableBoolean
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase

data class ParsingItem(val task : DownloaderTaskBase, val isSelected : ObservableBoolean) {

}