package moe.ore.txhook

import android.os.Bundle
import moe.ore.txhook.databinding.ActivityByteCheckBinding
import moe.ore.txhook.more.BaseActivity
import moe.ore.txhook.ui.main.ByteCheckPagerAdapter

class ByteCheckActivity: BaseActivity() {
    private lateinit var binding: ActivityByteCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityByteCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.also {
            val adapter = ByteCheckPagerAdapter(this, supportFragmentManager)
            it.adapter = adapter
            binding.tabs.setupWithViewPager(it)
        }

        binding.back.setOnClickListener {
            this.onBackPressed()
        }
    }
}