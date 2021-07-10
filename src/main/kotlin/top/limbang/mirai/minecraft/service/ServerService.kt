package top.limbang.mirai.minecraft.service

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Group
import top.limbang.doctor.client.MinecraftClient
import top.limbang.doctor.client.running.AutoVersionForgePlugin
import top.limbang.doctor.client.running.TpsPlugin
import top.limbang.doctor.client.running.tpsTools
import top.limbang.doctor.client.utils.ServerInfoUtils
import top.limbang.doctor.client.utils.substringBetween
import top.limbang.doctor.network.handler.onPacket
import top.limbang.doctor.network.handler.oncePacket
import top.limbang.doctor.protocol.definition.play.client.JoinGamePacket
import top.limbang.doctor.protocol.definition.play.client.PlayerPositionAndLookPacket
import top.limbang.mirai.minecraft.MinecraftPluginData
import top.limbang.mirai.minecraft.service.ImageService.createSubtitlesImage
import java.util.concurrent.TimeUnit

object ServerService {

    fun pingServer(name: String): String? {
        if (name.isEmpty()) return null
        val serverInfo = MinecraftPluginData.serverMap[name] ?: return null
        val serverListInfo = try {
            val json = MinecraftClient.ping(serverInfo.address, serverInfo.port)
                .get(5000, TimeUnit.MILLISECONDS) ?: return null
            ServerInfoUtils.getServiceInfo(json)
        } catch (e: Exception) {
            return null
        }

        var sampleName = ""
        serverListInfo.playerNameList.forEach { sampleName += "[${it}] " }

        var serverList = ""
        MinecraftPluginData.serverMap.forEach { serverList += "[${it.key}] " }

        return "服务器信息如下:\n" +
                "名   称: $name\n" +
                "版   本: ${serverListInfo.versionName}\n" +
                "描   述: ${serverListInfo.description}\n" +
                "在线人数: ${serverListInfo.playerOnline}/${serverListInfo.playerMax}\n" +
                "$sampleName\n" +
                "mod个数: ${serverListInfo.modNumber}\n" +
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