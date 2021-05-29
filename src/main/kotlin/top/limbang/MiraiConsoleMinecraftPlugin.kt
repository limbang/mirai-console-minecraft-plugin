package top.limbang

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.At
import top.limbang.doctor.client.MinecraftClient
import top.limbang.doctor.client.entity.ServiceInfo
import top.limbang.doctor.client.utils.ServiceInfoUtils
import top.limbang.utils.ImageErrorMessage


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
                handle(group, it, sender)
            }
            startsWith("！") {
                handle(group, it, sender)
            }
        }
    }

    private suspend fun handle(group: Group, mgs: String, sender: Member) {
        if (!mgs.endsWith("tps"))
            pingServer(group, mgs, sender)
        else
            getTps(group, mgs.substringBefore("tps").trim(), sender)
    }

    private suspend fun getTps(group: Group, mgs: String, sender: Member) {
        val serverInfo = PluginData.serverMap[mgs]!!

        val client = MinecraftClient()
            .user(serverInfo.loginInfo.username, serverInfo.loginInfo.password)
            .authServerUrl(serverInfo.loginInfo.authServerUrl)
            .sessionServerUrl(serverInfo.loginInfo.sessionServerUrl)
            .start(serverInfo.address, serverInfo.port)

    }


    private val errorMsgList = listOf(
        "希望睡醒服务器就好了.",
        "你把服务器玩坏了!!!",
        "等我喝口奶在试试.",
        "服务器连接不上,完蛋了.",
        "你干了啥?为啥连接不上.",
        "服务器故障!服务器故障!"
    )

    private suspend fun pingServer(group: Group, mgs: String, sender: Member) {
        if (mgs.isEmpty()) return
        val serverInfo = PluginData.serverMap[mgs] ?: return
        val serverListInfo: ServiceInfo
        try {
            val json = MinecraftClient.ping(serverInfo.address, serverInfo.port).get()
            serverListInfo = ServiceInfoUtils.getServiceInfo(json)
        } catch (e: Exception) {
            val randoms = (0..5).random()
            val sendMgs = sender.nameCard + errorMsgList[randoms]
            val image =
                ImageErrorMessage.createImage(
                    MiraiConsoleMinecraftPlugin.getResourceAsStream("$randoms.jpg")!!,
                    sendMgs
                )
            group.sendImage(image)
            return
        }
        var sampleName = ""
        serverListInfo.playerNameList.forEach {
            sampleName += "[${it}] "
        }

        var serverList = ""
        PluginData.serverMap.forEach {
            serverList += "[${it.key}] "
        }

        val sendMgs = "服务器信息如下:\n" +
                "名   称: $mgs\n" +
                "版   本: ${serverListInfo.versionName}\n" +
                "描   述: ${serverListInfo.description}\n" +
                "在线人数: ${serverListInfo.playerOnline}/${serverListInfo.playerMax}\n" +
                "$sampleName\n" +
                "mod个数: ${serverListInfo.modNumber}\n" +
                "服务器列表:$serverList"

        group.sendMessage(At(sender).plus(sendMgs))
    }
}

