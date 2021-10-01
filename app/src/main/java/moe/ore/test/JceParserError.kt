package moe.ore.test

import java.lang.Exception

class JceParserError private constructor(msg: String, val enum: JceParserErrorEnum) : Exception(msg) {
    companion object {
        val OFFSET_POS_ERROR = JceParserError("OFFSET_POS_ERROR", JceParserErrorEnum.OFFSET_POS_ERROR)
        val PARSER_FAILED = JceParserError("PARSER_FAILED", JceParserErrorEnum.PARSER_FAILED)
    }
}

enum class JceParserErrorEnum {
    OFFSET_POS_ERROR,
    PARSER_FAILED
}