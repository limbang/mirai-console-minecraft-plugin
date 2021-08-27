package top.limbang.mirai.minecraft.service

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import top.fanua.doctor.client.MinecraftClient
import top.fanua.doctor.client.entity.ServerInfo
import top.fanua.doctor.client.running.AutoVersionForgePlugin
import top.fanua.doctor.client.running.TpsPlugin
import top.fanua.doctor.client.running.tpsTools
import top.fanua.doctor.client.utils.ServerInfoUtils
import top.fanua.doctor.client.utils.substringBetween
import top.fanua.doctor.network.handler.onPacket
import top.fanua.doctor.network.handler.oncePacket
import top.fanua.doctor.protocol.definition.play.client.JoinGamePacket
import top.fanua.doctor.protocol.definition.play.client.PlayerPositionAndLookPacket
import top.limbang.mirai.minecraft.MinecraftPluginData
import top.limbang.mirai.minecraft.service.ImageService.createSubtitlesImage
import java.util.concurrent.TimeUnit

object ServerService {

    suspend fun pingALlServer(sender: Member, group: Group): Message {
        val builder = ForwardMessageBuilder(group)
        MinecraftPluginData.serverMap.forEach {
            try {
                val json = MinecraftClient.ping(it.value.address, it.value.port).get(5000, TimeUnit.MILLISECONDS)
                val serverInfo = ServerInfoUtils.getServiceInfo(json)
                builder.add(group.bot, PlainText(serverInfoToString(it.key, serverInfo)))
            } catch (e: Exception) {
                builder.add(
                    group.bot, PlainText("[${it.key}]服务器连接失败...\n").plus(
                        group.uploadImage(ImageService.createErrorImage(sender.nameCardOrNick), "jpg")
                    )
                )
            }
        }
        return builder.build()
    }

    fun pingServer(name: String): Any? {
        if (name.isEmpty()) return Unit
        val server = MinecraftPluginData.serverMap[name] ?: return Unit
        return pingServer(server.address, server.port,name)
    }

    fun pingServer(address:String,port:Int,name:String): Any? {
        val serverInfo = try {
            val json = MinecraftClient.ping(address,port)
                .get(5000, TimeUnit.MILLISECONDS) ?: return null
            ServerInfoUtils.getServiceInfo(json)
        } catch (e: Exception) {
            return null
        }
        return serverInfoToString(name, serverInfo)
    }

    private fun serverInfoToString(name: String, serverInfo: ServerInfo): String {
        var sampleName = ""
        serverInfo.playerNameList.forEach { sampleName += "[${it}] " }

        var serverList = ""
        MinecraftPluginData.serverMap.forEach { serverList += "[${it.key}] " }

        return "服务器信息如下:\n" +
                "名   称: $name\n" +
                "版   本: ${serverInfo.versionName}\n" +
                "描   述: ${serverInfo.description}\n" +
                "在线人数: ${serverInfo.playerOnline}/${serverInfo.playerMax}\n" +
                "$sampleName\n" +
                "mod个数: ${serverInfo.modNumber}\n" +
                "服务器列表:$serverList"
    }


    suspend fun getTPS(name: String, group: Group, sender: String) {
        val serverInfo = MinecraftPluginData.serverMap[name] ?: return
        if (serverInfo.loginInfo == null) {
            group.sendMessage("服务器[$name]未配置登陆信息...")
            return
        }

        val client = MinecraftClient.builder()
            .user(serverInfo.loginInfo.username, serverInfo.loginInfo.password)
            .authServerUrl(serverInfo.loginInfo.authServerUrl)
            .sessionServerUrl(serverInfo.loginInfo.sessionServerUrl)
            .plugin(TpsPlugin())
            .plugin(AutoVersionForgePlugin())
            .build()

        if (!client.start(serverInfo.address, serverInfo.port, 5000)) {
            group.sendImage(createSubtitlesImage("$sender,好像获取失败了哦！！！", "9.jpg"))
            return
        }

        client.oncePacket<JoinGamePacket> {
            GlobalScope.launch {
                group.sendMessage("登陆[$name]成功，开始发送 forge tps 指令")
            }
        }.onPacket<PlayerPositionAndLookPacket> {
            GlobalScope.launch {
                val forgeTps = client.tpsTools.getTpsSuspend()
                var outMsg = "[$name]低于20TPS的维度如下:\n"
                forgeTps.forEach { tpsEntity ->
                    val dim = tpsEntity.dim.substringBetween("Dim", "(").trim()
                    outMsg += when {
                        tpsEntity.dim == "Overall" -> "\n全局TPS:${tpsEntity.tps} Tick时间:${tpsEntity.tickTime}"
                        tpsEntity.tps < 20 -> "TPS:%-4.4s 维度:%s\n".format(tpsEntity.tps, dim)
                        else -> ""
                    }
                }
                group.sendMessage(outMsg)
                client.stop()
            }
        }
    }

    /**
     * ### 获取服务器列表
     */
    fun getServerList(): String {
        if (MinecraftPluginData.serverMap.isEmpty())
            return "无服务器列表..."
        var names = ""
        for ((name, address) in MinecraftPluginData.serverMap) {
            names += "[$name]${address.address}:${address.port}\n"
        }
        return "服务器列表为:\n$names"
    }
}