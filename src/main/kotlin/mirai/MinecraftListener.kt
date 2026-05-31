/*
 * Copyright 2020-2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/limbang/mirai-console-minecraft-plugin/blob/master/LICENSE
 */

package top.limbang.minecraft.mirai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import top.limbang.minecraft.entity.ServerStatus
import top.limbang.minecraft.mirai.PluginData.isPingToImg
import top.limbang.minecraft.mirai.PluginData.serverMap
import top.limbang.minecraft.ping
import top.limbang.minecraft.utlis.ServerStatusImageGenerator
import top.limbang.minecraft.utlis.toImage
import top.limbang.minecraft.utlis.toInput
import java.io.EOFException
import java.io.IOException

object MinecraftListener : SimpleListenerHost() {

    /**
     * 处理群消息事件，显示服务器列表。
     *
     * 当消息内容与指定命令匹配时，发送服务器列表到群聊。
     */
    @EventHandler
    fun GroupMessageEvent.list() {
        // 检查消息内容是否为指定的命令
        if (message.contentToString() != PluginData.commandMap[CommandName.LIST]) return

        // 创建响应消息
        val msg = if (serverMap.isEmpty()) {
            "无服务器列表..."
        } else {
            buildString {
                append("服务器列表为:\n")
                serverMap.forEach { (name, address) ->
                    append("[$name]${address.address}:${address.port}\n")
                }
            }
        }

        // 异步发送消息
        launch { group.sendMessage(msg) }
    }

    /**
     * 处理群消息事件，根据指定的命令 ping 服务器地址。
     *
     * 命令格式为 `!ping <address> <port>`，其中 `<port>` 是可选的。
     */
    @EventHandler
    fun GroupMessageEvent.pingAddress() {
        // 获取消息内容
        val content = message.contentToString()

        // 匹配命令格式，提取地址和端口号
        val regex =
            """^!ping\s?([\da-zA-Z.]*)\s?(?!0)(\d{1,4}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5])?${'$'}""".toRegex()
        val match = regex.find(content) ?: return
        val (address, tempPort) = match.destructured

        // 解析端口号，如果未提供，则默认使用 25565
        val port = tempPort.toIntOrNull() ?: 25565

        // 异步发送 ping 请求结果
        launch {
            if (isPingToImg) {
                group.sendMessage(pingServerImage(address, port, address))
            } else {
                group.sendMessage(pingServer(address, port, address))
            }
        }
    }

    /**
     * 处理群消息事件，执行 ping 操作并返回服务器状态。
     *
     * 命令格式为 [CommandName.PING]` <serverName>`，其中 `<serverName>` 是服务器名称。
     */
    @EventHandler
    fun GroupMessageEvent.ping() {
        // 获取消息内容
        val content = message.contentToString()

        // 匹配命令并提取服务器名称
        val regex = """^${PluginData.commandMap[CommandName.PING]}\s?(.*)""".toRegex()
        val match = regex.find(content) ?: return

        // 提取服务器名称
        val (name) = match.destructured

        // 查找服务器并发送 ping 请求
        serverMap[name]?.let { server ->
            launch {
                if (isPingToImg) {
                    group.sendMessage(pingServerImage(server.address, server.port, name))
                } else {
                    // 执行 ping 操作并获取结果，附加服务器列表
                    val pingResult = pingServer(server.address, server.port, name)
                    val serverList = getServerList()
                    group.sendMessage(pingResult + serverList)
                }
            }
        }
    }

    /**
     * 处理群消息事件，执行对所有服务器的 ping 操作并返回结果。
     *
     * 命令格式为 [CommandName.PING_ALL]，用于同时 ping 所有服务器。
     */
    @EventHandler
    fun GroupMessageEvent.pingAll() {
        // 检查消息内容是否为指定命令
        if (message.contentToString() != PluginData.commandMap[CommandName.PING_ALL]) return

        // 启动一个协程来处理Ping操作。
        launch {
            if (PluginData.isAllToImg) {
                if (serverMap.isEmpty()) {
                    group.sendMessage("无服务器列表...")
                    return@launch
                }

                val output = withContext(Dispatchers.IO) {
                    ServerStatusImageGenerator.generateFromPingList(
                        serverMap.map { (name, server) ->
                            ServerStatusImageGenerator.PingTarget(
                                serverName = name,
                                host = server.address,
                                port = server.port
                            )
                        }
                    )
                }
                group.sendMessage(group.uploadImage(output.toInput(), "png"))
                return@launch
            }

            // 使用 async 来并发处理每个服务器的 Ping 请求。
            val responses = serverMap.map { (name, ports) ->
                async(Dispatchers.IO) {
                    pingServer(ports.address, ports.port, name)
                }
            }.awaitAll()

            // 构建最终消息内容
            val message = buildString {
                responses.forEach { append(it) }
            }.plus(getServerList())

            // 直接发送文本消息
            group.sendMessage(message)
        }
    }


    /**
     * ping 服务器
     *
     * @param address 地址
     * @param port 端口
     * @param name 昵称
     * @return Message
     */
    private fun pingServer(address: String, port: Int, name: String): Message {
        return try {
            val (delay, serverStatus) = ping(address, port)
            serverStatus.toMessage(name, delay)
        } catch (e: EOFException) {
            Minecraft.logger.error("Ping服务器时遇到EOFException [$name] - 地址: $address:$port", e)
            PlainText("[$name] 获取服务器状态失败：服务器响应数据格式错误\n\n")
        } catch (e: IOException) {
            Minecraft.logger.error("Ping服务器时遇到IOException [$name] - 地址: $address:$port", e)
            PlainText("[$name] 获取服务器状态失败：I/O错误\n\n")
        } catch (e: Exception) {
            Minecraft.logger.error(
                "Ping服务器时遇到未知错误 [$name] - 地址: $address:$port - 错误信息: ${e.message}",
                e
            )
            PlainText("[$name] 获取服务器状态失败：${e.message}\n\n")
        }
    }

    /**
     * ping 服务器并把结果转换为图片消息。
     *
     * 该方法直接复用 [ServerStatusImageGenerator] 的图片生成能力，
     * 成功时返回状态图，失败时返回连接失败图。
     */
    private suspend fun GroupMessageEvent.pingServerImage(address: String, port: Int, name: String): Message {
        val output = ServerStatusImageGenerator.generateFromPing(
            serverName = name,
            host = address,
            port = port
        )
        return group.uploadImage(output.toInput(), "png")
    }

    /**
     * 把服务器信息转成消息
     *
     * @param name 昵称
     * @param delay 服务器延迟
     *
     * @return [Message]
     */
    private fun ServerStatus.toMessage(name: String, delay: Int): Message {
        var sampleName = ""
        playerInfo.players.forEach { sampleName += "[${it.name}] " }

        val serverStatus = PlainText(
            "服务器信息如下:\n" +
                    "名   称: $name\n" +
                    "延   迟: $delay ms\n" +
                    "版   本: ${versionName}\n" +
                    "描   述: ${descriptionColourHandle(description)}\n" +
                    "在线人数: ${playerInfo.playerOnline}/${playerInfo.playerMax}\n" +
                    "$sampleName\n" +
                    "mod个数: ${forgeData.mods.size}\n\n"
        )

        return serverStatus
    }

    /**
     * 获取服务器列表
     *
     * @return
     */
    private fun getServerList(): String {
        return "服务器列表: ${serverMap.entries.joinToString(separator = " ") { "[${it.key}]" }}"
    }

    /**
     * 服务器描述颜色处理
     */
    private fun descriptionColourHandle(description: String): String {
        return description.replace("""§[\da-z]""".toRegex(), "")
    }
}
