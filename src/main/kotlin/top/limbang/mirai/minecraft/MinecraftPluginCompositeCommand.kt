package top.limbang.mirai.minecraft

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.UserCommandSender
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.contact.User


/**
 * ### 插件指令
 */
object MinecraftPluginCompositeCommand : CompositeCommand(
    MiraiConsoleMinecraftPlugin, "mc",
    description = "添加删除服务器",
) {
    @SubCommand("addErrorAt", "添加错误At")
    suspend fun UserCommandSender.addErrorAt(user: User) {
        val group = getGroupOrNull()
        if (group == null) {
            sendMessage("本条消息只能在群配置.")
            return
        }
        val mutableList = MinecraftPluginData.adminMap[group.id]
        mutableList?.firstOrNull{it == user.id} ?: mutableList?.add(user.id)

        if (mutableList == null){
            MinecraftPluginData.adminMap[group.id] = mutableListOf<Long>().also { it.add(user.id) }
        }

        sendMessage("[${group.id}]群错误@提醒配置添加成功.")
    }

    @SubCommand("deleteErrorAt", "删除错误At")
    suspend fun UserCommandSender.deleteErrorAt(user: User) {
        val group = getGroupOrNull()
        if (group == null) {
            sendMessage("本条消息只能在群配置.")
            return
        }
        val mutableList =MinecraftPluginData.adminMap[group.id]

        if(mutableList != null){
            if(mutableList.remove(user.id)){
                sendMessage("[${group.id}]群错误@提醒配置删除成功.")
                return
            }
        }
        sendMessage("[${group.id}]群未找到[${user.id}]用户.")
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
            .also { MinecraftPluginData.loginMap[group.id] = it }
        sendMessage("[${group.id}]群默认登陆配置添加成功.")
    }

    @SubCommand("add", "添加")
    suspend fun UserCommandSender.add(name: String, address: String, port: Int) {
        val group = getGroupOrNull()
        if (group == null) {
            sendMessage("本条消息只能在群配置.")
            return
        }

        val loginInfo = MinecraftPluginData.loginMap[group.id]
        if (loginInfo == null) {
            sendMessage("[${group.id}]该群未添加默认的登陆信息.")
            return
        }

        MinecraftPluginData.serverMap[name] = ServerAddress(address, port, loginInfo)
        sendMessage("服务器[$name]添加成功.")
    }

    @SubCommand("add", "添加")
    suspend fun CommandSender.add(
        name: String, address: String, port: Int,
        authServerUrl: String, sessionServerUrl: String,
        username: String, password: String
    ) {
        MinecraftPluginData.serverMap[name] = ServerAddress(
            address, port, LoginInfo(
                authServerUrl, sessionServerUrl, username, password
            )
        )
        sendMessage("服务器[$name]添加成功.")
    }

    @SubCommand("delete", "删除")
    suspend fun CommandSender.delete(name: String) {
        if (MinecraftPluginData.serverMap.keys.remove(name)) {
            sendMessage("服务器[$name]删除成功.")
        } else {
            sendMessage("服务器[$name]删除失败.")
        }
    }

    @SubCommand("list", "列表")
    suspend fun CommandSender.list() {
        if (MinecraftPluginData.serverMap.isEmpty()) {
            sendMessage("无服务器列表...")
            return
        }
        var names = ""
        for ((name, address) in MinecraftPluginData.serverMap) {
            names += "[$name]地址:${address.address} 端口:${address.port}\n"
        }
        sendMessage("服务器列表为:\n$names")
    }
}