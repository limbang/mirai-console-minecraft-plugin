package top.limbang.mirai.minecraft

import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import top.limbang.mirai.minecraft.utils.PingUtils
import top.limbang.mirai.minecraft.utils.TPSUtils


object MiraiConsoleMinecraftPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "top.limbang.mirai-console-minecraft-plugin",
        version = "1.0.0",
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
    }

    private suspend fun handle(group: Group, mgs: String, sender: Member) {
        if (!mgs.endsWith("tps"))
            PingUtils.pingServer(group, mgs, sender)
        else
            TPSUtils.getTps(group, mgs.substringBefore("tps").trim())
    }

    fun sendMessage(group: Group, message: String) {
        launch {
            group.sendMessage(message)
        }
    }

}

