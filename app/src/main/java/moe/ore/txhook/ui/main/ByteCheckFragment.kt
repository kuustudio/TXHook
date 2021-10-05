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
import moe.ore.txhook.datas.PacketInfoData
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