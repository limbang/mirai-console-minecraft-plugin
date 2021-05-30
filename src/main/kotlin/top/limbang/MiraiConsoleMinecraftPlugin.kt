package top.limbang

import kotlinx.coroutines.launch
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
import top.limbang.doctor.client.entity.ServerInfo
import top.limbang.doctor.client.event.ChatEvent
import top.limbang.doctor.client.event.JoinGameEvent
import top.limbang.doctor.client.running.TpsEntity
import top.limbang.doctor.client.running.TpsUtils
import top.limbang.doctor.client.utils.ServerInfoUtils
import top.limbang.doctor.network.event.ConnectionEvent
import top.limbang.doctor.network.handler.onPacket
import top.limbang.doctor.protocol.definition.play.client.DisconnectPacket
import top.limbang.doctor.protocol.definition.play.client.PlayerPositionAndLookPacket
import top.limbang.doctor.protocol.entity.text.ChatGsonSerializer
import top.limbang.utils.ImageErrorMessage
import java.util.*


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

    private fun getTps(group: Group, mgs: String, sender: Member) {
        val serverInfo = PluginData.serverMap[mgs]!!

        val decode = Base64.getDecoder()
        val password = String(decode.decode(serverInfo.loginInfo.password))
        val client = MinecraftClient()
            .user(serverInfo.loginInfo.username,password)
            .authServerUrl(serverInfo.loginInfo.authServerUrl)
            .sessionServerUrl(serverInfo.loginInfo.sessionServerUrl)
            .start(serverInfo.address, serverInfo.port)

        val tpsList = mutableListOf<TpsEntity>()

        client.on(ChatEvent) {
            if (it.chatPacket.json.contains("commands.forge.tps.summary")) {
                val tpsEntity = TpsUtils.parseTpsEntity(it.chatPacket.json)
                tpsList.add(tpsEntity)
                if (tpsEntity.dim != "Overall") return@on

                var outMsg = "[XX服务器]低于20TPS如下:\n"
                tpsList.filterIndexed { index, tpsEntity ->
                    val dim = tpsEntity.dim.substringBetween("Dim", "(").trim()
                    outMsg += when {
                        index == tpsList.size - 1 -> {
                            "\n全局TPS:${tpsEntity.tps} Tick时间:${tpsEntity.tickTime}\n"
                        }
                        tpsEntity.tps < 20 -> "TPS:%-4.4s 维度:%s\n".format(tpsEntity.tps, dim)
                        else -> ""
                    }
                    true
                }
                println(outMsg)
                it.connection.close()
            }
        }.once(JoinGameEvent) {
            logger.info("登陆成功")
        }.onPacket<PlayerPositionAndLookPacket> {
            client.sendMessage("/forge tps")
        }


    }

    private fun sendMessage(group: Group, message: String) {
        launch {
            group.sendMessage(message)
        }
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
        val serverListInfo: ServerInfo
        try {
            val json = MinecraftClient.ping(serverInfo.address, serverInfo.port).get()
            if(json == null){
                sendErrorImage(group,mgs,sender)
                return
            }
            serverListInfo = ServerInfoUtils.getServiceInfo(json)
        } catch (e: Exception) {
            sendErrorImage(group,mgs,sender)
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

    private suspend fun sendErrorImage(group: Group, mgs: String, sender: Member){
        val randoms = (0..5).random()
        val sendMgs = sender.nameCard + errorMsgList[randoms]
        val image =
            ImageErrorMessage.createImage(
                MiraiConsoleMinecraftPlugin.getResourceAsStream("$randoms.jpg")!!,
                sendMgs
            )
        group.sendImage(image)
    }
}

