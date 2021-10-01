package moe.ore.txhook.ui.main

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xuexiang.xui.XUI
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView
import com.xuexiang.xui.widget.grouplist.XUIGroupListView
import com.xuexiang.xui.widget.textview.autofit.AutoFitTextView
import com.yuyh.jsonviewer.library.JsonRecyclerView
import moe.ore.tars.TarsBase.*
import moe.ore.tars.exc.TarsDecodeException
import moe.ore.test.JceParser
import moe.ore.test.JceParserError
import moe.ore.txhook.databinding.FragmentCatchBinding
import moe.ore.txhook.databinding.FragmentPacketAnayseBinding
import moe.ore.txhook.databinding.FragmentPacketDataBinding
import moe.ore.txhook.databinding.FragmentPacketInfoBinding
import moe.ore.txhook.datas.PacketInfoData
import moe.ore.txhook.datas.ProtocolDatas
import moe.ore.txhook.helper.toHexString
import moe.ore.txhook.more.copyText
import moe.ore.txhook.more.toast
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.lang.RuntimeException
import java.util.*

class PacketFragment(private val sectionNumber: Int, private val data: PacketInfoData) : Fragment() {
    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // println("unknown: $sectionNumber")
        when(sectionNumber) {
            1 -> {
                val binding = FragmentPacketInfoBinding.inflate(inflater, container, false)

                val copyListener = View.OnClickListener { (it as XUICommonListItemView).let { itemView ->
                    context?.copyText(itemView.detailText.toString())
                } }

                val groupListView = binding.groupListView

                val uinItem = groupListView.createItemView("Uin")
                uinItem.detailText = data.uin.toString()

                val cmdItem = groupListView.createItemView("Cmd")
                cmdItem.detailText = data.cmd

                val seqItem = groupListView.createItemView("Seq")
                seqItem.detailText = data.seq.toString()

                val timeItem = groupListView.createItemView("Time")
                timeItem.detailText = data.time.toString()

                val msgCookieItem = groupListView.createItemView("SessionId")
                msgCookieItem.detailText = data.sessionId.toHexString()

                val sizeItem = groupListView.createItemView("BufferSize")
                sizeItem.detailText = data.bufferSize.toString()

                val descItem = groupListView.createItemView("Desc")
                descItem.detailText = "未知作用"

                XUIGroupListView.newSection(context)
                    .setTitle("基础信息")
                    .addItemView(uinItem, copyListener)
                    .addItemView(cmdItem, copyListener)
                    .addItemView(seqItem, copyListener)
                    .addItemView(timeItem, copyListener)
                    .addItemView(msgCookieItem, copyListener)
                    .addItemView(sizeItem, copyListener)
                    .addItemView(descItem, copyListener)
                    .addTo(groupListView)

                if (data.packetType != -100) {
                // 默认packettype为-100，如果默认值不变则是发送的包
                    // 只有发送的包可以hook拿到packettype，其它没办法，在代码里面也只给发送的包传递bundles的时候设置了packettype
                    val packetTypeItem = groupListView.createItemView("PacketType")
                    packetTypeItem.detailText = data.packetType.toString() // 发包类型 有 0a 0b

                    val encodeTypeItem = groupListView.createItemView("EncodeType")
                    packetTypeItem.detailText = data.encodeType.toString() // 密钥类型 00 01 02 对应（无密钥，defaultkey，sessionkey，）

                    XUIGroupListView.newSection(context)
                        .setTitle("包体信息")
                        .addItemView(packetTypeItem, copyListener)
                        .addItemView(encodeTypeItem, copyListener)
                        .addTo(groupListView)
                }

                /*
                   from 的包体信息无法获取
                 */
                return binding.root
            }
            2 -> {
                val binding = FragmentPacketDataBinding.inflate(inflater, container, false)

                val dataView = binding.data
                dataView.typeface = XUI.getDefaultTypeface()

                var i = 0
                dataView.text = data.buffer.joinToString("") {
                    val str = (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase(Locale.getDefault())
                    i++
                    return@joinToString if (i == 8) {
                        i = 0
                        str + "\n"
                    } else "$str "
                } + "\n\n"

                dataView.setOnClickListener {
                    context?.copyText((it as AutoFitTextView).text.toString())
                }

                return binding.root
            }
            3 -> {
                val binding = FragmentPacketAnayseBinding.inflate(inflater, container, false)

                val buttonView = binding.buttonView

                if (data.bufferSize - 4 <= 0) {
                    buttonView.visibility = GONE
                    binding.emptyView.visibility = VISIBLE
                }


                var isLogin = false
                if (data.cmd.startsWith("wtlogin") || data.cmd.startsWith("wlogin")) {
                    binding.asPb.visibility = GONE
                    binding.asJce.text = "特殊分析"
                    isLogin = true
                }

                binding.json.setTextSize(18f)

                binding.asJce.setOnClickListener {
                    if (isLogin) { // 特殊分析模式

                    } else { // 普通分析jce
                        try {
                            // toast.show("开始分析")
                            val parser = JceParser(data.buffer, 4)
                            setData(binding.json, Gson().toJson(parser.value))

                            binding.dataView.visibility = VISIBLE
                            binding.buttonView.visibility = GONE

                            toast.show("分析成功")
                        } catch (e: Exception) {
                            toast.show("尝试作为Jce分析失败")
                        }
                    }
                }

                binding.asPb.setOnClickListener {


                }

                return binding.root
            }
        }
        return View(context)
    }

    private fun setData(view: JsonRecyclerView, json: String) {
        val arr = JsonParser().parse(json).asJsonObject["values"].asJsonArray

        view.bindJson(convertData(arr))
    }

    private fun convertData(arr: JsonArray): JSONObject {
        val data = JSONObject()

        arr.forEach {
            val elem = it.asJsonObject
            convertValue(data, elem)
        }

        return data
    }

    private fun convertValue(data: JSONObject, elem: JsonObject) {
        val tag = elem["tag"].asInt
        data.put(tag.toString(), convertValue(elem))
    }

    private fun convertValue(elem: JsonObject): Any {
        return when(val type = elem["type"].asByte) {
            LONG -> elem["number"].asLong
            DOUBLE -> elem["double"].asDouble
            STRING1 -> elem["string"].asString
            SIMPLE_LIST -> "[hex]" + elem["string"].asString
            STRUCT_BEGIN -> {
                val ar = JsonParser().parse(elem["json"].asString).asJsonObject["values"].asJsonArray
                val out = convertData(ar)
                out
            }
            LIST -> {
                val `in` = JSONObject()
                val ar = JsonParser().parse(elem["json"].asString).asJsonArray
                ar.forEach {
                    val e = it.asJsonObject
                    convertValue(`in`, e)
                }
                `in`
            }
            MAP -> {
                val out = JSONObject()
                val j = JsonParser().parse(elem["json"].asString).asJsonObject
                j.entrySet().forEach {
                    val key = it.key
                    val value = it.value.asJsonObject
                    out.put(key, convertValue(value))
                }
                out
            }
            else -> throw RuntimeException("unknown type: $type")
        }
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int, data: PacketInfoData): PacketFragment {
            return PacketFragment(sectionNumber, data).apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}