package top.limbang

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.UserCommandSender
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.contact.User


/**
 * ### 插件指令
 */
object PluginCompositeCommand : CompositeCommand(
    MiraiConsoleMinecraftPlugin, "mc",
    description = "添加删除服务器",
) {
    @SubCommand("admin", "管理")
    suspend fun UserCommandSender.admin(user: User) {
        val group = getGroupOrNull()
        if (group == null) {
            sendMessage("本条消息只能在群配置.")
            return
        }

        PluginData.adminMap[group.id]?.add(user.id)
        if (PluginData.adminMap[group.id] == null){
            PluginData.adminMap[group.id] = mutableListOf<Long>().also { it.add(user.id) }
        }

        sendMessage("[${group.id}]群管理配置添加成功.")
    }

    @SubCommand("login", "登陆")
    suspend fun UserCommandSender.login(
        authServerUrl: String, sessionServerUrl: String, username: String, password: String
    ) {
        val group = getGroupOrNull()
        if (group == null) {
            sendMessage("本条消息只能在群配置.")
            return
        }
        LoginInfo(authServerUrl, sessionServerUrl, username, password)
            .also { PluginData.loginMap[group.id] = it }
        sendMessage("[${group.id}]群默认登陆配置添加成功.")
    }

    @SubCommand("add", "添加")
    suspend fun UserCommandSender.add(name: String, address: String, port: Int) {
        val group = getGroupOrNull()
        if (group == null) {
            sendMessage("本条消息只能在群配置.")
            return
        }

        val loginInfo = PluginData.loginMap[group.id]
        if (loginInfo == null) {
            sendMessage("[${group.id}]该群未添加默认的登陆信息.")
            return
        }

        PluginData.serverMap[name] = ServerAddress(address, port, loginInfo)
        sendMessage("服务器[$name]添加成功.")
    }

    @SubCommand("add", "添加")
    suspend fun CommandSender.add(
        name: String, address: String, port: Int,
        authServerUrl: String, sessionServerUrl: String,
        username: String, password: String
    ) {
        PluginData.serverMap[name] = ServerAddress(
            address, port, LoginInfo(
                authServerUrl, sessionServerUrl, username, password
            )
        )
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