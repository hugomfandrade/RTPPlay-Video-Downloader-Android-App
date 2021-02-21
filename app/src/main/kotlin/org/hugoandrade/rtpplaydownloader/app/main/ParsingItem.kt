package org.hugoandrade.rtpplaydownloader.app.main

import androidx.databinding.ObservableBoolean
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTask

data class ParsingItem(val task : ParsingTask, val isSelected : ObservableBoolean) {

}