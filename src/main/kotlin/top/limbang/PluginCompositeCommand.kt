package top.limbang

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand


/**
 * ### 插件指令
 */
object PluginCompositeCommand : CompositeCommand(
    MiraiConsoleMinecraftPlugin, "mc",
    description = "添加删除服务器",
) {

    @SubCommand("add", "添加")
    suspend fun CommandSender.add(name: String, address: String, port: Int) {
        PluginData.serverMap[name] = ServerAddress(address, port)
        sendMessage("服务器[$name]添加成功.")
    }

    @SubCommand("delete", "删除")
    suspend fun CommandSender.delete(name: String) {
        if (PluginData.serverMap.keys.remove(name)) {
            sendMessage("服务器[$name]删除成功.")
        } else {
            sendMessage("服务器[$name]删除失败.")
        }
    }

    @SubCommand("list", "列表")
    suspend fun CommandSender.list() {
        if (PluginData.serverMap.isEmpty()) {
            sendMessage("无服务器列表...")
            return
        }
        var names = ""
        for ((name, address) in PluginData.serverMap) {
            names += "[$name]地址:${address.address} 端口:${address.port}\n"
        }
        sendMessage("服务器列表为:\n$names")
    }
}