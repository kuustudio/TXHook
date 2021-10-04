package moe.ore.txhook.catching

import moe.ore.txhook.helper.EMPTY_BYTE_ARRAY

data class ToService(
    var fromSource: FromSource,
    var uin: Long,
    var seq: Int,
    var cmd: String,
    var buffer: ByteArray,
    var time: Long,
    var sessionId: ByteArray,
): PacketService(false, true) {
    var encodeType: Byte = 0
    var packetType: Int = 0

    var firstToken: ByteArray = EMPTY_BYTE_ARRAY
    var secondToken: ByteArray = EMPTY_BYTE_ARRAY
}
