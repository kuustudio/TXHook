package moe.ore.txhook

import android.os.Bundle
import moe.ore.txhook.more.BaseActivity
import moe.ore.txhook.ui.main.PacketPagerAdapter
import moe.ore.txhook.databinding.ActivityPacketInfoBinding

class PacketInfoActivity : BaseActivity() {
    private lateinit var binding: ActivityPacketInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout binding
        binding = ActivityPacketInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind view pager
        binding.viewPager.also {

            // Get adapter
            val adapter = PacketPagerAdapter(
                this,
                supportFragmentManager,
                intent.getParcelableExtra("data")!!
            )

            // Set adapter
            it.adapter = adapter
            binding.tabs.setupWithViewPager(it)
        }

        // On back pressed
        binding.back.setOnClickListener {
            this.onBackPressed()
        }
    }
}
