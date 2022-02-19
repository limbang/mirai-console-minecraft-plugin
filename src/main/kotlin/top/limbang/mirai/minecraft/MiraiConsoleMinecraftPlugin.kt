package top.limbang.mirai.minecraft


import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import top.limbang.mirai.minecraft.service.ImageService.createErrorImage
import top.limbang.mirai.minecraft.service.ServerService.getServerList
import top.limbang.mirai.minecraft.service.ServerService.getTPS
import top.limbang.mirai.minecraft.service.ServerService.pingALlServer
import top.limbang.mirai.minecraft.service.ServerService.pingServer


object MiraiConsoleMinecraftPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "top.limbang.mirai-console-minecraft-plugin",
        version = "1.1.7",
    ) {
        author("limbang")
    }
) {
    override fun onDisable() {
        MinecraftPluginCompositeCommand.unregister()
    }

    override fun onEnable() {
        MinecraftPluginData.reload()
        MinecraftPluginCompositeCommand.register()
        val ping = MinecraftPluginData.commandMap[CommandName.PING] ?: "!"
        val list = MinecraftPluginData.commandMap[CommandName.LIST] ?: "!list"
        val tps = MinecraftPluginData.commandMap[CommandName.TPS] ?: "!tps"
        val pingAll = MinecraftPluginData.commandMap[CommandName.PING_ALL] ?: "!all"

        globalEventChannel().subscribeGroupMessages {
            case(list) quoteReply { getServerList() }
            case(pingAll) reply { pingALlServer(sender,group) }
            startsWith(ping) quoteReply {
                pingServer(it.substringAfter(ping).trim()) ?: subject.uploadImage(createErrorImage(sender.nameCardOrNick), "jpg")
            }
            startsWith(tps) { getTPS(it, group, sender.nameCardOrNick) }
            startsWith("!ping") quoteReply {
                val parameter = it.trim().split(Regex("\\s"))
                if(parameter.size < 2 || parameter.size > 3) return@quoteReply "参数不正确:!ping <地址> [端口]"
                val address = parameter[1]
                val port = if(parameter.size == 3 ) parameter[2].toInt() else 25565
                pingServer(address,port,address) ?: subject.uploadImage(createErrorImage(sender.nameCardOrNick), "jpg")
            }
        }

        globalEventChannel().subscribeAlways<NudgeEvent> {
            if (target.id == bot.id) {
                subject.sendMessage(
                    "Minecraft 插件使用说明:\n" +
                            "Ping服务器:$ping 服务器名称\n" +
                            "Ping服务器:!ping <地址> [端口]\n"+
                            "Ping所有服务器:$pingAll\n" +
                            "TPS:$tps 服务器名称\n" +
                            "查看服务器列表:$list"
                )
            }
        }
    }


}

