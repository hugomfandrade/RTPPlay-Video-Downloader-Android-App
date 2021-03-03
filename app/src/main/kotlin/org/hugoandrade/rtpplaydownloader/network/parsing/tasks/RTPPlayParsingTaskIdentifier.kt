package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

class RTPPlayParsingTaskIdentifier : ParsingTaskDelegate(listOf(
        RTPPlayParsingTaskV7(),
        RTPPlayParsingTaskV6(),
        RTPPlayParsingTaskV5(),
        RTPPlayParsingTaskV4(),
        RTPPlayParsingTaskV3(),
        RTPPlayParsingTaskV2(),
        RTPPlayParsingTask()))