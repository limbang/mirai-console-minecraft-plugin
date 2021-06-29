package top.limbang.mirai.minecraft

import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import top.limbang.mirai.minecraft.utils.PingUtils
import top.limbang.mirai.minecraft.utils.TPSUtils


object MiraiConsoleMinecraftPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "top.limbang.mirai-console-minecraft-plugin",
        version = "1.0.4",
    ) {
        author("limbang")
        info("""Minecraft""")
    }
) {
    override fun onDisable() {
        MinecraftPluginCompositeCommand.unregister()
    }

    override fun onEnable() {
        MinecraftPluginData.reload()
        MinecraftPluginCompositeCommand.register()
        globalEventChannel().subscribeGroupMessages {
            startsWith("!") {
                handle(group, it, sender)
            }
            startsWith("！") {
                handle(group, it, sender)
            }
        }
        globalEventChannel().subscribeAlways<NudgeEvent> {
            if (target.id == bot.id) {
                subject.sendMessage(
                    "Minecraft 插件使用说明:\n" +
                            "!为中英文通用,!和服务器名称之间可以有空格,和tps之间也一样\n" +
                            "Ping服务器:!前缀加服务器名称,如!服务器名称\n" +
                            "TPS:!前缀加服务器名称,tps结尾,如!服务器名称tps\n" +
                            "查看服务器列表:!list"
                )
            }
        }
    }

    /**
     * 处理ping服务器,和登录服务器发送tps命令
     */
    private suspend fun handle(group: Group, mgs: String, sender: Member) {
        if (mgs == "list") {
            group.sendMessage(getServerList())
            return
        }
        launch {
            if (!mgs.endsWith("tps"))
                PingUtils.pingServer(group, mgs, sender)
            else
                TPSUtils.getTps(group, mgs.substringBefore("tps").trim(), sender)
        }
    }

    fun sendMessage(group: Group, message: String) {
        launch {
            group.sendMessage(message)
        }
    }

    fun getServerList(): String {
        if (MinecraftPluginData.serverMap.isEmpty())
            return "无服务器列表..."
        var names = ""
        for ((name, address) in MinecraftPluginData.serverMap) {
            names += "[$name]地址:${address.address} 端口:${address.port}\n"
        }
        return "服务器列表为:\n$names"
    }
}

