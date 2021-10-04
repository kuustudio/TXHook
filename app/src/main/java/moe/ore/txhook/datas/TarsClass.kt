package moe.ore.txhook.datas

import moe.ore.tars.TarsBase
import moe.ore.tars.TarsInputStream
import moe.ore.tars.TarsOutputStream
import moe.ore.txhook.catching.FromSource
import moe.ore.txhook.helper.EMPTY_BYTE_ARRAY

class HookSetting: TarsBase() {
    var autoSsoLoginMerge: Boolean = true

    override fun writeTo(output: TarsOutputStream) {
        output.write(autoSsoLoginMerge, 0)
    }

    override fun readFrom(input: TarsInputStream) {
        autoSsoLoginMerge = input.read(autoSsoLoginMerge, 0, false)
    }
}

class KeyList: TarsBase() {
    var publicKeyList = arrayListOf<ByteArray>()
    var shareKeyList = arrayListOf<ByteArray>()

    override fun readFrom(input: TarsInputStream) {
        shareKeyList = (input.read(cache_bytes_list, 0, false) as? ArrayList<ByteArray>) ?: publicKeyList
        publicKeyList = (input.read(cache_bytes_list, 1, false) as? ArrayList<ByteArray>) ?: shareKeyList
    }

    override fun writeTo(output: TarsOutputStream) {
        output.write(shareKeyList, 0)
        output.write(publicKeyList, 1)
    }

    companion object {
        private val cache_bytes_list = arrayListOf(EMPTY_BYTE_ARRAY)
    }
}

class Packet: TarsBase() {
    var fromSource: FromSource = FromSource.MOBILE_QQ
    var uin: Long = 0
    var seq: Int = 0
    var cmd: String = ""
    var buffer: ByteArray = EMPTY_BYTE_ARRAY
    var time: Long = 0
    var sessionId: ByteArray = EMPTY_BYTE_ARRAY

    var encodeType: Byte = 0
    var packetType: Int = 0

    var firstToken: ByteArray = EMPTY_BYTE_ARRAY
    var secondToken: ByteArray = EMPTY_BYTE_ARRAY

    override fun writeTo(output: TarsOutputStream) {
        output.write(fromSource.name, 0)
        output.write(uin, 1)
        output.write(seq, 2)
        output.write(cmd, 3)
        output.write(buffer, 4)
        output.write(time, 5)
        output.write(sessionId, 6)
        // output.write(fromSource, 1)
        output.write(encodeType, 6)
        output.write(packetType, 7)
        output.write(firstToken, 8)
        output.write(secondToken, 9)
    }

    override fun readFrom(input: TarsInputStream) {
        fromSource = FromSource.valueOf(input.readString(0, true))
        uin = input.read(uin, 1, false)
        seq = input.read(seq, 2, false)
        cmd = input.read(cmd, 3, false)
        buffer = input.read(buffer, 4, false)
        time = input.read(time, 5, false)
        sessionId = input.read(sessionId, 6, false)
        // uin = input.read(uin, 1, false)
        encodeType = input.read(encodeType, 6, false)
        packetType = input.read(packetType, 7, false)
        firstToken = input.read(firstToken, 8, false)
        secondToken = input.read(secondToken, 9, false)
    }

    fun parse(byteArray: ByteArray): Packet {
        readFrom(TarsInputStream(byteArray))
        return this
    }
}
