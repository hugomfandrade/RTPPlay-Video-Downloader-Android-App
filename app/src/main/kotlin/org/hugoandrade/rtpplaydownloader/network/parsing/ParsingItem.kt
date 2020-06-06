package org.hugoandrade.rtpplaydownloader.network.parsing

import androidx.databinding.ObservableBoolean
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTaskBase

data class ParsingItem(val task : ParsingTaskBase, val isSelected : ObservableBoolean) {

}