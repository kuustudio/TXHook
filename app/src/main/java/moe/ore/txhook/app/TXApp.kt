package moe.ore.txhook.app

import android.widget.ListView
import androidx.constraintlayout.widget.ConstraintLayout
import com.xuexiang.xui.widget.searchview.MaterialSearchView
import moe.ore.txhook.R
import moe.ore.txhook.catching.PacketService
import moe.ore.txhook.databinding.FragmentCatchBinding
import moe.ore.txhook.more.BaseApp

class TXApp: BaseApp() {
    override fun isDebug(): Boolean {
        return true
    }

    companion object {
        val catchingList = arrayListOf<PacketService>()
        lateinit var catching: FragmentCatchBinding

        /**
         * 获取抓包列表
         */
        fun getCatchingList(): ListView {
            return catching.multipleStatusView.contentView.findViewById(R.id.catch_list)
        }

        fun getCatchingSearchBar(): MaterialSearchView {
            return catching.multipleStatusView.contentView.findViewById(R.id.search_bar)
        }
    }
}