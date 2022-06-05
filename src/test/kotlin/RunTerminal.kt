import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import top.limbang.minecraft.Minecraft
import java.io.File
import java.util.*

suspend fun main() {
    MiraiConsoleTerminalLoader.startAsDaemon()

    val pluginInstance = Minecraft

    pluginInstance.load() // 主动加载插件, Console 会调用 MinecraftRemoteConsole.onLoad
    pluginInstance.enable() // 主动启用插件, Console 会调用 MinecraftRemoteConsole.onEnable

    val properties = Properties().apply { File("account.properties").inputStream().use { load(it) } }

    val bot = MiraiConsole.addBot(properties.getProperty("id").toLong(), properties.getProperty("password")).alsoLogin() // 登录一个测试环境的 Bot

    MiraiConsole.job.join()
}
