package moe.ore.txhook

import android.os.Bundle
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.yuyh.jsonviewer.library.JsonRecyclerView
import moe.ore.tars.TarsBase
import moe.ore.txhook.databinding.ActivityJsonViewBinding
import moe.ore.txhook.more.BaseActivity
import org.json.JSONObject
import java.lang.RuntimeException

class JsonViewActivity: BaseActivity() {
    private lateinit var binding: ActivityJsonViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout binding
        binding = ActivityJsonViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.json.bindJson(intent.getStringExtra("data") ?: "{}")

        // On back pressed
        binding.back.setOnClickListener {
            this.onBackPressed()
        }
    }
}
