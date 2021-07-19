package top.limbang.mirai.minecraft

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.UserCommandSender
import net.mamoe.mirai.console.command.getGroupOrNull
import top.limbang.mirai.minecraft.service.ServerService.getServerList


/**
 * ### 插件指令
 */
object MinecraftPluginCompositeCommand : CompositeCommand(
    MiraiConsoleMinecraftPlugin, "mc"
) {
    @SubCommand
    suspend fun CommandSender.setCommand(name: CommandName,command:String) {
        MinecraftPluginData.commandMap[name] = command
        sendMessage("配置[$name]触发指令[$command],需重启机器人生效.")
    }


    @SubCommand
    suspend fun CommandSender.loginInfo() {
        var loginInfo = ""
        MinecraftPluginData.loginMap.forEach {
            loginInfo += "[${it.key}] "
        }
        if (loginInfo.isEmpty())
            sendMessage("无登陆信息...")
        else
            sendMessage("登陆配置信息名称:$loginInfo")
    }

    @SubCommand
    suspend fun UserCommandSender.addLogin(
        name: String, authServerUrl: String, sessionServerUrl: String, username: String, password: String
    ) {
        val group = getGroupOrNull()
        if (group != null) {
            sendMessage("此配置不能在群配置.")
            return
        }
        LoginInfo(authServerUrl, sessionServerUrl, username, password)
            .also { MinecraftPluginData.loginMap[name] = it }
        sendMessage("[$name]登陆配置添加成功.")
    }

    @SubCommand
    suspend fun CommandSender.deleteLogin(name: String) {
        if (MinecraftPluginData.loginMap.keys.remove(name)) {
            sendMessage("登陆配置[$name]删除成功.")
        } else {
            sendMessage("登陆配置[$name]删除失败.")
        }
    }

    @SubCommand
    suspend fun UserCommandSender.add(name: String, address: String, port: Int) {
        MinecraftPluginData.serverMap[name] = ServerAddress(address, port, null)
        sendMessage("服务器[$name]添加成功.")
    }

    @SubCommand
    suspend fun UserCommandSender.add(name: String, address: String, port: Int, loginName: String) {

        val loginInfo = MinecraftPluginData.loginMap[loginName]
        if (loginInfo == null) {
            sendMessage("[$loginName]该登陆信息尚未配置.")
            return
        }

        MinecraftPluginData.serverMap[name] = ServerAddress(address, port, loginInfo)
        sendMessage("服务器[$name]添加成功.")
    }

    @SubCommand
    suspend fun CommandSender.delete(name: String) {
        if (MinecraftPluginData.serverMap.keys.remove(name)) {
            sendMessage("服务器[$name]删除成功.")
        } else {
            sendMessage("服务器[$name]删除失败.")
        }
    }

    @SubCommand
    suspend fun CommandSender.list() {
        sendMessage(getServerList())
    }
}