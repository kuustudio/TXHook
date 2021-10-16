package moe.ore.txhook.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xuexiang.xui.XUI
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog.SingleButtonCallback
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView.ACCESSORY_TYPE_CHEVRON
import com.xuexiang.xui.widget.grouplist.XUIGroupListView
import kotlinx.io.core.discardExact
import kotlinx.io.core.readBytes
import moe.ore.txhook.app.TXApp
import moe.ore.txhook.catching.PacketService
import moe.ore.txhook.databinding.FragmentCatchBinding
import moe.ore.txhook.databinding.FragmentDataBinding
import moe.ore.txhook.databinding.FragmentSettingBinding
import moe.ore.txhook.datas.PacketInfoData
import moe.ore.txhook.datas.ProtocolDatas
import moe.ore.txhook.helper.toByteReadPacket
import moe.ore.txhook.helper.toHexString
import moe.ore.txhook.ui.list.CatchingBaseAdapter
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView.ACCESSORY_TYPE_SWITCH
import moe.ore.txhook.databinding.FragmentToolsBinding
import com.xuexiang.xui.widget.searchview.MaterialSearchView
import moe.ore.txhook.*

import moe.ore.txhook.helper.ThreadManager
import moe.ore.txhook.helper.fastTry
import moe.ore.txhook.more.*
import java.io.File
import kotlin.concurrent.thread


/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment(private val sectionNumber: Int, private val uiHandler: Handler) : Fragment() {
    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // println("unknown: $sectionNumber")
        when(sectionNumber) {
            1 -> {
                val binding = FragmentCatchBinding.inflate(inflater, container, false).also { TXApp.catching = it }

                val statusView = binding.multipleStatusView
                statusView.setOnRetryClickListener {
                    CookieBars.cookieBar(activity, "提示一下", "请点击右下角按钮刷新获取数据哦~", "明白了") {
                        toast.show("去吧，皮卡丘~")
                    }
                }

                val adapter = CatchingBaseAdapter(TXApp.catchingList)
                val listView = TXApp.getCatchingList()
                listView.adapter = adapter

                listView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->

                    val service = parent.getItemAtPosition(position) as PacketService
                    val startIntent = Intent(XUI.getContext(), PacketInfoActivity::class.java)

                    startIntent.putExtra("data", PacketInfoData().apply {
                        if (service.from) {
                            val from = service.toFromService()
                            fromSource = from.fromSource
                            uin = from.uin
                            seq = from.seq
                            cmd = from.cmd
                            bufferSize = from.buffer.size
                            buffer = from.buffer
                            time = from.time
                            sessionSize = from.sessionId.size
                            sessionId = from.sessionId
                        } else {
                            val to = service.toToService()
                            fromSource = to.fromSource
                            uin = to.uin
                            seq = to.seq
                            cmd = to.cmd
                            bufferSize = to.buffer.size
                            buffer = to.buffer
                            time = to.time
                            sessionSize = to.sessionId.size
                            sessionId = to.sessionId

                            packetType = to.packetType
                            encodeType = to.encodeType
                        }
                    })
                    startActivity(startIntent)
                }

                if (TXApp.catchingList.isEmpty()) statusView.showEmpty() else statusView.showContent()

                TXApp.getCatchingSearchBar().let { mSearchView ->
                    mSearchView.setVoiceSearch(false)
                    mSearchView.setEllipsize(true)
                    mSearchView.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener {
                        override fun onSearchViewShown() {
                            uiHandler.let {
                                it.handleMessage(Message.obtain(it, MainActivity.UI_CHANGE_SEARCH_BUTTON, 1, 0))
                            }
                        }

                        override fun onSearchViewClosed() {
                            uiHandler.let {
                                it.handleMessage(Message.obtain(it, MainActivity.UI_CHANGE_SEARCH_BUTTON, 0, 0))
                            }
                        }
                    })
                    mSearchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String): Boolean {
                            uiHandler.let {
                                it.handleMessage(Message.obtain(it, MainActivity.UI_FILTER_CATCHING_BY_MATCH, 1, 0, query))
                            }
                            return false
                        }

                        override fun onQueryTextChange(newText: String): Boolean {
                            return false
                        }
                    })
                }

                return binding.root
            }
            2 -> {
                val binding = FragmentDataBinding.inflate(inflater, container, false)

                val copyListener = View.OnClickListener { (it as XUICommonListItemView).let { item ->
                    context?.copyText(item.detailText.toString())
                } }

                val baseInfoList = binding.groupListView

                val aidItem = baseInfoList.addTextItem("AppId", ProtocolDatas.getAppId().toString())
                val maxPackageSizeItem = baseInfoList.addTextItem("MaxPackageSize", ProtocolDatas.getMaxPackageSize().toString())
                val publicKeyItem = baseInfoList.addTextItem("PublicKey", "点击查看详细")
                val shareKeyItem = baseInfoList.addTextItem("ShareKey", "点击查看详细")
                val guidItem = baseInfoList.addTextItem("Guid", ProtocolDatas.getGUID().toHexString())
                val ksidItem = baseInfoList.addTextItem("Ksid", ProtocolDatas.getKsid().toHexString())
                val qimeiItem = baseInfoList.addTextItem("QImei", ProtocolDatas.getQIMEI().toHexString())

                XUIGroupListView.newSection(context)
                    .setTitle("基础信息")
                    .addItemView(aidItem, copyListener)
                    .addItemView(maxPackageSizeItem, copyListener)
                    .addItemView(publicKeyItem) {
                        val list = ProtocolDatas.getKeyList()
                        if (list.publicKeyList.isNotEmpty()) {
                            val arr = list.publicKeyList.map { it.toHexString() }
                            MaterialDialog.Builder(requireContext())
                                .title("密钥列表（点击复制）")
                                .items(
                                    arr.map { it.let { if (it.length > 24) it.substring(0, 24) + "..." else it } }
                                )
                                .itemsCallback { dialog: MaterialDialog, _: View?, position: Int, _: CharSequence? ->
                                    dialog.dismiss()
                                    context?.copyText(arr[position])
                                }
                                .show()
                        } else {
                            toast.show("啥数据也没有嗷~~")
                        }
                    }
                    .addItemView(shareKeyItem) {
                        val list = ProtocolDatas.getKeyList()
                        if (list.shareKeyList.isNotEmpty()) {
                            val arr = list.shareKeyList.map { it.toHexString() }
                            MaterialDialog.Builder(requireContext())
                                .title("密钥列表（点击复制）")
                                .items(
                                    arr.map { it.let { if (it.length > 24) it.substring(0, 24) + "..." else it } }
                                )
                                .itemsCallback { dialog: MaterialDialog, _: View?, position: Int, _: CharSequence? ->
                                    dialog.dismiss()
                                    context?.copyText(arr[position])
                                }
                                .show()
                        } else {
                            toast.show("空空如也~~")
                        }
                    }
                    .addItemView(guidItem, copyListener)
                    .addItemView(ksidItem, copyListener)
                    .addItemView(qimeiItem, copyListener)
                    .addTo(baseInfoList)

                val lastUin = String(ProtocolDatas.getId("last_uin"))

                val reader = ProtocolDatas.getId("$lastUin-AccountKey").toByteReadPacket()
                if (reader.hasBytes(18)) {
                    val uinItem = baseInfoList.createItemView("Uin")
                    uinItem.detailText = lastUin

                    reader.discardExact(reader.readShort().toInt())

                    val a1Item = baseInfoList.addTextItem("A1", reader.readBytes(reader.readShort().toInt()).toHexString())

                    val a2Item = baseInfoList.addTextItem("A2", reader.readBytes(reader.readShort().toInt()).toHexString())
                    val a3Item = baseInfoList.addTextItem("A3", reader.readBytes(reader.readShort().toInt()).toHexString())

                    val d1Item = baseInfoList.addTextItem("D1", reader.readBytes(reader.readShort().toInt()).toHexString())
                    val d2Item = baseInfoList.addTextItem("D2", reader.readBytes(reader.readShort().toInt()).toHexString())

                    val s2Item = baseInfoList.addTextItem("S2", reader.readBytes(reader.readShort().toInt()).toHexString())
                    val keyItem = baseInfoList.addTextItem("Key", reader.readBytes(reader.readShort().toInt()).toHexString())
                    val cookieItem = baseInfoList.addTextItem("Cookie", reader.readBytes(reader.readShort().toInt()).toHexString())

                    XUIGroupListView.newSection(context)
                        .setTitle("最后QQ操作者TOKEN")
                        .addItemView(uinItem, copyListener)
                        .addItemView(a1Item, copyListener)
                        .addItemView(a2Item, copyListener)
                        .addItemView(a3Item, copyListener)
                        .addItemView(d1Item, copyListener)
                        .addItemView(d2Item, copyListener)
                        .addItemView(s2Item, copyListener)
                        .addItemView(keyItem, copyListener)
                        .addItemView(cookieItem, copyListener)
                        .addTo(baseInfoList)
                }

                val svnItem = baseInfoList.createItemView("SvnVersion")
                svnItem.detailText = ProtocolDatas.getSVNVersion()

                val releaseTimeItem = baseInfoList.createItemView("ReleaseTime")
                releaseTimeItem.detailText = ProtocolDatas.getReleaseTime()

                val androidIdItem = baseInfoList.createItemView("AndroidId")
                androidIdItem.detailText = ProtocolDatas.getAndroidId()

                val macItem = baseInfoList.createItemView("MacAddress")
                macItem.detailText = ProtocolDatas.getMac()

                val ssidItem = baseInfoList.createItemView("SSID")
                ssidItem.detailText = ProtocolDatas.getSsid()

                val bssidItem = baseInfoList.createItemView("BSSID")
                bssidItem.detailText = ProtocolDatas.getBSsid()

                val netTypeItem = baseInfoList.createItemView("NetType")
                netTypeItem.detailText = ProtocolDatas.getNetType().toString()

                val logDirItem = baseInfoList.createItemView("LogPath")
                logDirItem.detailText = ProtocolDatas.getWloginLogDir()

                XUIGroupListView.newSection(context)
                    .setTitle("QQ其它参数")
                    .addItemView(svnItem, copyListener)
                    .addItemView(releaseTimeItem, copyListener)
                    .addItemView(androidIdItem, copyListener)
                    .addItemView(macItem, copyListener)
                    .addItemView(ssidItem, copyListener)
                    .addItemView(bssidItem, copyListener)
                    .addItemView(netTypeItem, copyListener)
                    .addItemView(logDirItem, copyListener)
                    .addTo(baseInfoList)

                return binding.root
            }
            3 -> {
                val binding = FragmentToolsBinding.inflate(inflater, container, false)
                binding.analyseView.setOnClickListener {
                    val intent = Intent(requireContext(), JsonViewActivity::class.java)
                    intent.putExtra("require_input", false)
                    requireContext().startActivity(intent)
                }
                binding.calcView.setOnClickListener {
                    requireContext().startActivity(Intent(requireContext(), ByteCheckActivity::class.java))
                }

                return binding.root
            }
            4 -> {
                val binding = FragmentSettingBinding.inflate(inflater, container, false)

                val group = binding.groupListView

                config.changeViewRefresh

                val maxPacketSizeItem = group.createItemView("最大包展示大小")
                maxPacketSizeItem.detailText = config.maxPacketSize.toString()
                maxPacketSizeItem.accessoryType = ACCESSORY_TYPE_CHEVRON

                val changeViewRefreshItem = group.createItemView("切换界面自动刷新(卡顿)")
                changeViewRefreshItem.accessoryType = ACCESSORY_TYPE_SWITCH
                changeViewRefreshItem.switch.isChecked = config.changeViewRefresh
                changeViewRefreshItem.switch.setOnCheckedChangeListener { _, isChecked ->
                    config.changeViewRefresh = isChecked
                    config.apply()
                    toast.show("修改为：${config.changeViewRefresh}")
                }

                val autoLoginMerge = group.createItemView("自动翻译Sso.LoginMerge")
                autoLoginMerge.accessoryType = ACCESSORY_TYPE_SWITCH
                autoLoginMerge.switch.isChecked = ProtocolDatas.getSetting().autoSsoLoginMerge
                autoLoginMerge.switch.setOnCheckedChangeListener { _, isChecked ->
                    val setting = ProtocolDatas.getSetting()
                    setting.autoSsoLoginMerge = isChecked
                    ProtocolDatas.setSetting(setting)
                }

                val cleanCache = group.createItemView("清空所有缓存")
                cleanCache.accessoryType = ACCESSORY_TYPE_CHEVRON

                XUIGroupListView.newSection(context)
                    .setTitle("基础设置")
                    .addItemView(maxPacketSizeItem) {
                        MaterialDialog.Builder(requireContext())
                            .iconRes(R.drawable.ic_baseline_edit_note_24)
                            .title("请输入修改后的值")
                            .content("取值范围在0~2000之间，切记不要设置的过大！")
                            .inputType(InputType.TYPE_CLASS_NUMBER)
                            .input("请输入一个数字", "", false, (MaterialDialog.InputCallback { _: MaterialDialog?, text: CharSequence ->
                                val num = text.toString().toInt()
                                config.maxPacketSize = num
                                config.apply()
                                maxPacketSizeItem.detailText = text
                                toast.show("修改成功~")
                            }))
                            .inputRange(1, 4)
                            .positiveText("确定")
                            .negativeText("取消")
                            .onPositive((SingleButtonCallback { dialog: MaterialDialog, _: DialogAction? -> dialog.dismiss() }))
                            .cancelable(false)
                            .show()
                    }
                    .addItemView(changeViewRefreshItem, null)
                    .addItemView(autoLoginMerge, null)
                    .addItemView(cleanCache) {
                        val dataPath = File(ProtocolDatas.dataPath)
                        dataPath.deleteRecursively()
                    }
                    .addTo(group)

                return binding.root
            }
        }
        return View(context)
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
        fun newInstance(sectionNumber: Int, uiHandler: Handler): PlaceholderFragment {
            return PlaceholderFragment(sectionNumber, uiHandler).apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}
