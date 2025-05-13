package org.hugoandrade.downloader.parsing

data class TSUrl(var url : String,
                 val bandwidth : Int? = null,
                 val resolution : IntArray? = null) {

}