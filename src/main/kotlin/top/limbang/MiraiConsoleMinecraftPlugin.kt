package top.limbang

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import top.limbang.doctor.client.MinecraftClient


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
        PluginCompositeCommand.unregister()
    }

    override fun onEnable() {
        PluginData.reload()
        PluginCompositeCommand.register()
        globalEventChannel().subscribeGroupMessages {
            startsWith("!") {

            }
            startsWith("！") {

            }
        }
    }

    private fun pingServer(group: Group, mgs: String, sender: Member) {
        if (mgs.isEmpty()) return
        val serverInfo = PluginData.serverMap[mgs] ?: return
        val json = MinecraftClient.ping(serverInfo.address, serverInfo.port).get()




        logger.info(json)

    }
}

