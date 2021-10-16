package moe.ore.txhook.more

import com.xuexiang.xui.widget.grouplist.XUICommonListItemView
import com.xuexiang.xui.widget.grouplist.XUIGroupListView

fun XUIGroupListView.addTextItem(name: String, detail: String): XUICommonListItemView {
    val item = createItemView(name)
    item.detailText = detail
    return item
}