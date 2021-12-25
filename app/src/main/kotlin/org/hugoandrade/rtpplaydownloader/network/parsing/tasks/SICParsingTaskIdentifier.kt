package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

class SICParsingTaskIdentifier : ParsingTaskDelegate(listOf(
        SICParsingTaskV4(),
        SICParsingTaskV3(),
        SICParsingTaskV2(),
        SICParsingTaskV1()))