package org.hugoandrade.rtpplaydownloader.app.main

import androidx.databinding.ObservableBoolean
import org.hugoandrade.downloader.parsing.ParsingData

data class ParsingItem(val parsingData: ParsingData, val isSelected : ObservableBoolean)