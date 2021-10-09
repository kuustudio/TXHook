package moe.ore.txhook

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.HorizontalScrollView
import android.widget.RelativeLayout
import com.xuexiang.xui.widget.edittext.materialedittext.MaterialEditText
import com.xuexiang.xui.widget.layout.XUILinearLayout
import com.yuyh.jsonviewer.library.ProtocolViewer
import moe.ore.test.ProtobufParser
import moe.ore.test.TarsParser
import moe.ore.txhook.databinding.ActivityJsonViewBinding
import moe.ore.txhook.helper.HexUtil
import moe.ore.txhook.helper.fastTry
import moe.ore.txhook.more.BaseActivity
import moe.ore.txhook.more.toast

class JsonViewActivity: BaseActivity() {
    private lateinit var binding: ActivityJsonViewBinding

    private lateinit var inputView: XUILinearLayout
    private lateinit var jsonView: HorizontalScrollView
    private lateinit var json: ProtocolViewer
    private lateinit var input: MaterialEditText
    private lateinit var buttonView: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout binding
        binding = ActivityJsonViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inputView = binding.inputView
        jsonView = binding.jsonView
        json = binding.json
        input = binding.etInput
        buttonView = binding.buttonView

        if (intent.getBooleanExtra("require_input", true)) {
            buttonView.visibility = GONE
            loadJson(intent.getStringExtra("data") ?: "{}")
        } else {
            binding.asJce.setOnClickListener {
                fastTry {
                    val bytes = HexUtil.Hex2Bin(input.text.toString())
                    if (bytes.isNotEmpty()) {
                        val parser = TarsParser(bytes)
                        loadJson(parser.startParsing().toString())
                    }
                }.onFailure {
                    toast.show("分析失败")
                }
            }
            binding.asPb.setOnClickListener {
                fastTry {
                    val parser = ProtobufParser(HexUtil.Hex2Bin(input.text.toString()))
                    loadJson(parser.startParsing().toString())
                }.onFailure {
                    toast.show("分析失败")
                }
            }
            binding.reanayse.setOnClickListener {
                jsonView.visibility = GONE
                inputView.visibility = VISIBLE

                input.clear()
            }
        }

        // On back pressed
        binding.back.setOnClickListener {
            this.onBackPressed()
        }
    }

    private fun loadJson(str: String) {
        inputView.visibility = GONE
        jsonView.visibility = VISIBLE
        json.bindJson(str)
    }
}
