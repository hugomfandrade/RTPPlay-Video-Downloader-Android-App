package org.hugoandrade.downloader.parsing.tasks

class RTPPlayParsingTaskIdentifier : org.hugoandrade.downloader.parsing.tasks.ParsingTaskDelegate(listOf(
        RTPPlayParsingTaskV8(),
        RTPPlayParsingTaskV7(),
    org.hugoandrade.downloader.parsing.tasks.RTPPlayParsingTaskV6(),
        RTPPlayParsingTaskV5(),
        RTPPlayParsingTaskV4(),
        RTPPlayParsingTaskV3(),
        RTPPlayParsingTaskV2(),
        RTPPlayParsingTaskV1()))