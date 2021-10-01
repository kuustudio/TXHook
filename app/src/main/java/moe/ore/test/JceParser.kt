package moe.ore.test

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.readBytes
import moe.ore.tars.TarsBase
import moe.ore.tars.exc.TarsDecodeException
import moe.ore.txhook.helper.sub
import moe.ore.txhook.helper.toByteReadPacket
import moe.ore.txhook.helper.toHexString
import java.lang.Exception
import kotlin.jvm.Throws

class JceParser
    @Throws(JceParserError::class)
    constructor(data: ByteArray, pos: Int) {

    val buffer: ByteArray =
        if (pos == 0)
            data
        else data.sub(pos, data.size - pos).let { it ?: throw JceParserError.OFFSET_POS_ERROR }

    val value: JceValues = parserByReader(buffer.toByteReadPacket())

    // @Throws(JceParserError::class)
    private fun parserByReader(reader: ByteReadPacket): JceValues {
        val values = JceValues()
        try {
            val list = values.values
            while (reader.hasBytes(1)) {
                val hd = HeadData()
                readHead(hd, reader)
                when (hd.type) {
                    STRUCT_END -> return values
                    ZERO_TAG -> list.add(Value().apply {
                        this.type = LONG
                        this.tag = hd.tag
                        this.number = 0
                    })
                    BYTE -> list.add(Value().apply {
                        this.type = LONG
                        this.tag = hd.tag
                        this.number = reader.readByte().toLong()
                    })
                    SHORT -> list.add(Value().apply {
                        this.type = LONG
                        this.tag = hd.tag
                        this.number = reader.readShort().toLong()
                    })
                    INT -> list.add(Value().apply {
                        this.type = LONG
                        this.tag = hd.tag
                        this.number = reader.readInt().toLong()
                    })
                    LONG -> list.add(Value().apply {
                        this.type = LONG
                        this.tag = hd.tag
                        this.number = reader.readLong()
                    })
                    FLOAT -> list.add(Value().apply {
                        this.type = DOUBLE
                        this.tag = hd.tag
                        this.double = reader.readFloat().toDouble()
                    })
                    DOUBLE -> list.add(Value().apply {
                        this.type = DOUBLE
                        this.tag = hd.tag
                        this.double = reader.readDouble()
                    })
                    STRING1 -> list.add(Value().apply {
                        this.type = STRING1
                        this.tag = hd.tag
                        var len: Int = reader.readByte().toInt()
                        if (len < 0) len += 256
                        val ss = reader.readBytes(len)
                        this.string = String(ss)
                    })
                    STRING4 -> list.add(Value().apply {
                        this.type = STRING1
                        this.tag = hd.tag
                        val len: Int = reader.readInt()
                        if (len > TarsBase.MAX_STRING_LENGTH || len < 0) throw TarsDecodeException("String too long: $len")
                        val ss = reader.readBytes(len)
                        this.string = String(ss)
                    })
                    SIMPLE_LIST -> list.add(Value().apply {
                        this.type = SIMPLE_LIST
                        this.tag = hd.tag

                        val hh = HeadData()
                        readHead(hh, reader)
                        if (hh.type != BYTE) { // 只有bytes的array使用simple_list
                            throw TarsDecodeException("type mismatch, tag: " + tag + ", type: " + hd.type + ", " + hh.type)
                        }
                        val size = reader.getJceLong().toInt()
                        if (size < 0)
                            throw TarsDecodeException("invalid size, tag: " + tag + ", type: " + hd.type + ", " + hh.type + ", size: " + size)

                        val bytes = reader.readBytes(size)

                        this.string = bytes.toHexString(false)
                    })
                    STRUCT_BEGIN -> list.add(Value().apply {
                        this.type = STRUCT_BEGIN
                        this.tag = hd.tag

                        val jceValues = parserByReader(reader)

                        this.json = Gson().toJson(jceValues)
                    })
                    LIST -> list.add(Value().apply {
                        this.type = LIST
                        this.tag = hd.tag

                        val size = reader.getJceLong().toInt()
                        if (size < 0)
                            throw TarsDecodeException("invalid size, tag: " + tag + ", type: " + hd.type + ", " + hd.type + ", size: " + size)

                        val array = JsonArray()
                        repeat(size) {
                            val obj = Value()

                            val hh = HeadData()
                            readHead(hh, reader)

                            obj.tag = hh.tag

                            when (hh.type) {
                                ZERO_TAG -> {
                                    obj.type = LONG
                                    obj.number = 0
                                }
                                BYTE -> {
                                    obj.type = LONG
                                    obj.number = reader.readByte().toLong()
                                }
                                SHORT -> {
                                    obj.type = LONG
                                    obj.number = reader.readShort().toLong()
                                }
                                INT -> {
                                    obj.type = LONG
                                    obj.number = reader.readInt().toLong()
                                }
                                LONG -> {
                                    obj.type = LONG
                                    obj.number = reader.readLong()
                                }
                                FLOAT -> {
                                    obj.type = DOUBLE
                                    obj.double = reader.readFloat().toDouble()
                                }
                                DOUBLE -> {
                                    obj.type = DOUBLE
                                    obj.double = reader.readDouble()
                                }
                                STRING1 ->  {
                                    obj.type = STRING1
                                    var len: Int = reader.readByte().toInt()
                                    if (len < 0) len += 256
                                    val ss = reader.readBytes(len)
                                    obj.string = String(ss)
                                }
                                STRING4 -> {
                                    obj.type = STRING1
                                    val len: Int = reader.readInt()
                                    if (len > TarsBase.MAX_STRING_LENGTH || len < 0) throw TarsDecodeException("String too long: $len")
                                    val ss = reader.readBytes(len)
                                    obj.string = String(ss)
                                }
                                STRUCT_BEGIN -> {
                                    obj.type = STRUCT_BEGIN
                                    val jceValues = parserByReader(reader)
                                    obj.json = Gson().toJson(jceValues)
                                }
                            }

                            array.add(Gson().toJsonTree(obj))
                        }

                        this.json = array.toString()
                    })
                    MAP -> list.add(Value().apply {
                        this.type = MAP
                        this.tag = hd.tag

                        val objects = JsonObject()
                        val size = reader.getJceLong().toInt()
                        if (size < 0) throw TarsDecodeException("size invalid: $size")

                        repeat(size) {
                            val k = reader.getJceValue()

                            val key = if (k is Long || k is Double || k is String) {
                                k.toString()
                            } else {
                                throw TarsDecodeException("unknown jce map key")
                            }

                            val hh = HeadData()
                            readHead(hh, reader)

                            val value = Value()

                            when (hh.type) {
                                ZERO_TAG -> {
                                    value.type = LONG
                                    value.number = 0
                                }
                                BYTE -> {
                                    value.type = LONG
                                    value.number = reader.readByte().toLong()
                                }
                                SHORT -> {
                                    value.type = LONG
                                    value.number = reader.readShort().toLong()
                                }
                                INT -> {
                                    value.type = LONG
                                    value.number = reader.readInt().toLong()
                                }
                                LONG -> {
                                    value.type = LONG
                                    value.number = reader.readLong()
                                }
                                FLOAT -> {
                                    value.type = DOUBLE
                                    value.double = reader.readFloat().toDouble()
                                }
                                DOUBLE -> {
                                    value.type = DOUBLE
                                    value.double = reader.readDouble()
                                }
                                STRING1 ->  {
                                    var len: Int = reader.readByte().toInt()
                                    if (len < 0) len += 256
                                    val ss = reader.readBytes(len)

                                    value.type = STRING1
                                    value.string = String(ss)
                                }
                                STRING4 -> {
                                    val len: Int = reader.readInt()
                                    if (len > TarsBase.MAX_STRING_LENGTH || len < 0) throw TarsDecodeException("String too long: $len")
                                    val ss = reader.readBytes(len)

                                    value.type = STRING1
                                    value.string = String(ss)
                                }
                                STRUCT_BEGIN -> {
                                    val jceValues = parserByReader(reader)

                                    value.type = STRUCT_BEGIN
                                    value.json = Gson().toJson(jceValues)
                                }
                            }

                            objects.add(key, Gson().toJsonTree(value))
                        }

                        this.json = objects.toString()

                    })
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            throw JceParserError.PARSER_FAILED
        }
        return values
    }

    private inline fun ByteReadPacket.getJceString(): String {
        val jce = getJceValue()
        if (jce is String) {
            return jce
        } else {
            throw TarsDecodeException("getJceLong value is not long")
        }
    }

    private inline fun ByteReadPacket.getJceLong(): Long {
        val jce = getJceValue()
        if (jce is Long) {
            return jce
        } else {
            throw TarsDecodeException("getJceLong value is not long, is a $jce")
        }
    }

    private fun ByteReadPacket.getJceValue(): Any {
        val hd = HeadData()
        readHead(hd, this)
        return when (hd.type) {
            ZERO_TAG -> 0L
            BYTE -> readByte().toLong()
            SHORT -> readShort().toLong()
            INT -> readInt().toLong()
            LONG -> readLong()
            FLOAT -> readFloat().toDouble()
            DOUBLE -> readDouble()
            STRING1 ->  {
                var len: Int = readByte().toInt()
                if (len < 0) len += 256
                val ss = readBytes(len)
                return String(ss)
            }
            STRING4 -> {
                val len: Int = readInt()
                if (len > TarsBase.MAX_STRING_LENGTH || len < 0) throw TarsDecodeException("String too long: $len")
                val ss = readBytes(len)
                return String(ss)
            }
            STRUCT_BEGIN -> {
                val jceValues = parserByReader(this)

                return Gson().toJson(jceValues)
            }
            else -> hd
        }
    }

    private fun readHead(hd: HeadData, bb: ByteReadPacket): Int {
        val b = bb.readByte().toInt()
        hd.type = (b and 15).toByte()
        hd.tag = b and (15 shl 4) shr 4
        if (hd.tag == 15) {
            hd.tag = bb.readByte().toInt() and 0x00ff
            return 2
        }
        return 1
    }

    class HeadData {
        var type: Byte = 0
        var tag = 0
    }

    companion object {
        const val BYTE: Byte = 0
        const val SHORT: Byte = 1
        const val INT: Byte = 2
        const val LONG: Byte = 3
        const val FLOAT: Byte = 4
        const val DOUBLE: Byte = 5
        const val STRING1: Byte = 6
        const val STRING4: Byte = 7
        const val MAP: Byte = 8
        const val LIST: Byte = 9
        const val STRUCT_BEGIN: Byte = 10
        const val STRUCT_END: Byte = 11
        const val ZERO_TAG: Byte = 12
        const val SIMPLE_LIST: Byte = 13
    }
}

class JceValues {
    var values = arrayListOf<Value>()
}

class Value {
    var tag: Int = 0

    var type: Byte = -1

    var number: Long = 0

    var string: String = ""

    var double: Double = 0.0

    var json: String = ""
}