package top.limbang

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info

object MiraiConsoleMinecraftPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "top.limbang.mirai-console-minecraft-plugin",
        version = "1.0.0",
    ) {
        author("limbang")
        info("""Minecraft""")
    }
) {
    override fun onEnable() {
        logger.info { "Plugin loaded" }
    }
}