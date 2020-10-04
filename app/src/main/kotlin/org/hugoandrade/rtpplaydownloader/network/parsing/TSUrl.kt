package org.hugoandrade.rtpplaydownloader.network.parsing

data class TSUrl(val url : String,
                 val bandwidth : Int? = null,
                 val resolution : IntArray? = null) {

}