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
import top.limbang.mirai.minecraft.service.ServerService.pingServer


object MiraiConsoleMinecraftPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "top.limbang.mirai-console-minecraft-plugin",
        version = "1.1.1",
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

        globalEventChannel().subscribeGroupMessages {
            (startsWith("!") or startsWith("！")).quoteReply {
                val mgs = it.substring(1)
                if (mgs == "list" || mgs == "列表") getServerList()
                else if (mgs.endsWith("tps")) {
                    getTPS(mgs.substringBefore("tps").trim(),group,sender.nameCardOrNick)
                    Unit
                } else {
                    pingServer(mgs) ?: group.uploadImage(createErrorImage(sender.nameCardOrNick),"jpg")
                }
            }
        }

        globalEventChannel().subscribeAlways<NudgeEvent> {
            if (target.id == bot.id) {
                subject.sendMessage(
                    "Minecraft 插件使用说明:\n" +
                            "!为中英文通用,!和服务器名称之间可以有空格,和tps之间也一样\n" +
                            "Ping服务器:!前缀加服务器名称,如!服务器名称\n" +
                            "TPS:!前缀加服务器名称,tps结尾,如!服务器名称tps\n" +
                            "查看服务器列表:!list 或 !列表"
                )
            }
        }
    }

}

