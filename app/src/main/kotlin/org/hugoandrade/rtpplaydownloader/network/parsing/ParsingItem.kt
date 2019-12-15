package org.hugoandrade.rtpplaydownloader.network.parsing

import android.databinding.ObservableBoolean
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTaskBase

data class ParsingItem(val task : ParsingTaskBase, val isSelected : ObservableBoolean) {

}