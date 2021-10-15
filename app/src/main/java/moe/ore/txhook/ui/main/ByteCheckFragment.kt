package moe.ore.txhook.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import moe.ore.txhook.R
import moe.ore.txhook.databinding.FragmentByteCheckBinding
import moe.ore.txhook.databinding.FragmentCryptorBinding
import moe.ore.txhook.databinding.FragmentTransformBinding
import moe.ore.txhook.datas.ProtocolDatas
import moe.ore.txhook.helper.*
import moe.ore.txhook.more.copyText
import moe.ore.txhook.more.toast
import java.security.MessageDigest
import java.util.zip.CRC32

class ByteCheckFragment(private val sectionNumber: Int) : Fragment() {
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
        when(sectionNumber) {
            1 -> {
                val binding = FragmentByteCheckBinding.inflate(inflater, container, false)

                val contentInput = binding.etInput
                val md5Input = binding.md5Input
                val sha1Input = binding.sha1Input
                val sha256Input = binding.sha256Input
                val crc32Input = binding.crc32Input

                fun emptyKey() {
                    md5Input.setText(R.string.example_md5_hex)
                    sha1Input.setText(R.string.example_md5_hex)
                    sha256Input.setText(R.string.example_md5_hex)
                    crc32Input.setText(R.string.example_md5_hex)
                }

                binding.md5Copy.setOnClickListener { requireContext().copyText(md5Input.editValue) }
                binding.sha1Copy.setOnClickListener { requireContext().copyText(sha1Input.editValue) }
                binding.sha256Copy.setOnClickListener { requireContext().copyText(sha256Input.editValue) }
                binding.crc32Copy.setOnClickListener { requireContext().copyText(crc32Input.editValue) }

                contentInput.addTextChangedListener {
                    if (it != null) {
                        val str = it.toString()
                        if (str.isBlank()) emptyKey()

                        fastTry {
                            val bs = str.hex2ByteArray()

                            binding.hexLength.text = bs.size.toString()

                            val sha1 = MessageDigest.getInstance("SHA")
                            val sha256 = MessageDigest.getInstance("SHA-256")

                            val crc32 = CRC32().also { it.update(bs) }

                            md5Input.setText(MD5.toMD5Byte(bs).toHexString())
                            sha1Input.setText(sha1.digest(bs).toHexString())
                            sha256Input.setText(sha256.digest(bs).toHexString())
                            crc32Input.setText( BytesUtil.int64ToBuf32(crc32.value).toHexString() )

                        }.onFailure {
                            toast.show("输入内容有误，请正确输入HEX：${it.message}")
                        }
                    } else {
                        emptyKey()
                    }
                }

                return binding.root
            }
            2 -> {
                val binding = FragmentTransformBinding.inflate(inflater, container, false)

                val hexTransInput = binding.transHexInput
                binding.hex2str.setOnClickListener {
                    fastTry {
                        val str = hexTransInput.editValue.hex2ByteArray()
                        hexTransInput.setText(String(str))
                    }.onFailure {
                        toast.show("转换错误：${it.message}")
                    }
                }
                binding.str2hex.setOnClickListener {
                    val str = hexTransInput.editValue
                    hexTransInput.setText(str.toByteArray().toHexString())
                }

                val zipInput = binding.transZipInput
                binding.zip.setOnClickListener {
                    fastTry {
                        val str = zipInput.editValue.hex2ByteArray()
                        zipInput.setText(ZipUtil.compress(str).toHexString())
                    }.onFailure {
                        toast.show("转换错误：${it.message}")
                    }
                }
                binding.unzip.setOnClickListener {
                    fastTry {
                        val str = zipInput.editValue.hex2ByteArray()
                        zipInput.setText(ZipUtil.unCompress(str).toHexString())
                    }.onFailure {
                        toast.show("转换错误：${it.message}")
                    }
                }
                binding.gzip.setOnClickListener {
                    fastTry {
                        val str = zipInput.editValue.hex2ByteArray()
                        zipInput.setText(GZIPUtils.compress(str).toHexString())
                    }.onFailure {
                        toast.show("转换错误：${it.message}")
                    }
                }
                binding.ungzip.setOnClickListener {
                    fastTry {
                        val str = zipInput.editValue.hex2ByteArray()
                        zipInput.setText(GZIPUtils.uncompress(str).toHexString())
                    }.onFailure {
                        toast.show("转换错误：${it.message}")
                    }
                }

                return binding.root
            }
            3 -> {
                val binding = FragmentCryptorBinding.inflate(inflater, container, false)

                val input = binding.input
                val output = binding.output
                val key = binding.key

                val item = binding.cryptorSpinner

                binding.enc.setOnClickListener {
                    when(item.selectedIndex) {
                        0 -> { // tea
                            fastTry {
                                val k = key.editValue
                                val data = (input.editValue ?: return@setOnClickListener).hex2ByteArray()
                                if (k.isNullOrBlank()) {
                                    output.setText(TeaUtil.encrypt(data, ByteArray(16)).toHexString())
                                } else {
                                    output.setText(TeaUtil.encrypt(data, k.hex2ByteArray()).toHexString())
                                }
                            }.onFailure { toast.show("加密失败：${it.message}") }
                        }

                    }
                }
                binding.dec.setOnClickListener {
                    when(item.selectedIndex) {
                        0 -> { // tea
                            fastTry {
                                val k = key.editValue
                                val data = (input.editValue ?: return@setOnClickListener).hex2ByteArray()
                                if (k.isNullOrBlank()) {
                                    val keys = ProtocolDatas.getKeyList()

                                    var isSuccess = false

                                    keys.shareKeyList.also {
                                        it.add(0, ByteArray(16))
                                    }.forEach {
                                        val out = TeaUtil.decrypt(data, it)
                                        if (out != null) {
                                            key.setText(it.toString())
                                            output.setText(out.toHexString())
                                            isSuccess = true
                                            toast.show("解密成功，密钥已展出：sharekey")
                                            return@forEach
                                        }
                                    }

                                    /*
                                    if (!isSuccess) {
                                        ProtocolDatas.get
                                        key.setText(it.toString())
                                        output.setText(out.toHexString())
                                        isSuccess = true
                                        toast.show("解密成功，密钥已展出 尝试d2")
                                    } */


                                    if (!isSuccess) toast.show("解密失败：无对应密钥")

                                } else {
                                    output.setText( TeaUtil.decrypt(
                                        data,
                                        k.hex2ByteArray()
                                    ).toHexString())
                                }
                            }.onFailure { toast.show("解密失败：${it.message}") }
                        }

                    }
                }

                return binding.root
            }
        }
        return View(context)
    }

    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int): ByteCheckFragment {
            return ByteCheckFragment(sectionNumber).apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}