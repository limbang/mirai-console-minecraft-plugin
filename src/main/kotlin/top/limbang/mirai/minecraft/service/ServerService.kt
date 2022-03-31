/*
 * Copyright 2020-2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/limbang/mirai-console-minecraft-plugin/blob/master/LICENSE
 */

package top.limbang.mirai.minecraft.service

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import top.fanua.doctor.client.MinecraftClient
import top.fanua.doctor.client.entity.ServerInfo
import top.fanua.doctor.client.running.AutoVersionForgePlugin
import top.fanua.doctor.client.running.tps.TpsPlugin
import top.fanua.doctor.client.running.tps.tpsTools
import top.fanua.doctor.client.utils.ServerInfoUtils
import top.fanua.doctor.client.utils.substringBetween
import top.fanua.doctor.network.handler.onPacket
import top.fanua.doctor.network.handler.oncePacket
import top.fanua.doctor.plugin.fix.PluginFix
import top.fanua.doctor.protocol.definition.play.client.JoinGamePacket
import top.fanua.doctor.protocol.definition.play.client.PlayerPositionAndLookPacket
import top.limbang.mirai.minecraft.MinecraftPluginData
import top.limbang.mirai.minecraft.service.ImageService.createSubtitlesImage
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object ServerService {

    /**
     * ping 所有服务器
     *
     * @param group
     * @return
     */
    fun GroupMessageEvent.pingALlServer(): ForwardMessage {
        return buildForwardMessage {
            MinecraftPluginData.serverMap.forEach {
                bot says (pingServer(it.value.address, it.value.port, it.key) ?: PlainText("[${it.key}]服务器连接失败...\n"))
            }
        }
    }

    /**
     * 根据预设好的名称 ping 服务器
     *
     * @param name 名称
     * @return
     */
    fun pingServer(name: String): Any? {
        if (name.isEmpty()) return Unit
        val server = MinecraftPluginData.serverMap[name] ?: return Unit
        return pingServer(server.address, server.port, name)
    }

    /**
     * ping 服务器
     *
     * @param address 地址
     * @param port 端口
     * @param name 昵称
     * @return
     */
    fun pingServer(address: String, port: Int, name: String): Message? {
        return ServerInfoUtils.getServiceInfo(
            try {
                MinecraftClient.ping(address, port).get(5000, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                println("获取ping信息,等待超时...")
                return null
            } catch (e: ExecutionException) {
                println("获取ping信息失败,${e.message}")
                return null
            }
        ).toMessage(name)
    }

    /**
     * 把服务器信息转成消息
     *
     * @param name 昵称
     * @return [Message]
     */
    private fun ServerInfo.toMessage(name: String): Message {
        var sampleName = ""
        playerNameList.forEach { sampleName += "[${it}] " }

        var serverList = ""
        MinecraftPluginData.serverMap.forEach { serverList += "[${it.key}] " }

        return PlainText(
            "服务器信息如下:\n" +
                    "名   称: $name\n" +
                    "版   本: ${versionName}\n" +
                    "描   述: ${descriptionColourHandle(description)}\n" +
                    "在线人数: ${playerOnline}/${playerMax}\n" +
                    "$sampleName\n" +
                    "mod个数: ${modNumber}\n" +
                    "服务器列表:$serverList"
        )
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
            .plugin(AutoVersionForgePlugin())
            .plugin(TpsPlugin())
            .plugin(PluginFix())
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

    /**
     * 服务器描述颜色处理
     */
    private fun descriptionColourHandle(description: String): String {
        return description.replace("§[0-9a-z]".toRegex(), "")
    }
}