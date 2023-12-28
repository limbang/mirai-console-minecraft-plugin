/*
 * Copyright 2020-2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/limbang/mirai-console-minecraft-plugin/blob/master/LICENSE
 */

package top.limbang.minecraft.mirai

import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildForwardMessage
import top.limbang.minecraft.entity.ServerStatus
import top.limbang.minecraft.mirai.PluginData.serverMap
import top.limbang.minecraft.ping
import top.limbang.minecraft.utlis.toImage
import top.limbang.minecraft.utlis.toInput


object MinecraftListener : SimpleListenerHost() {

    /**
     * 显示服务器列表
     *
     */
    @EventHandler
    fun GroupMessageEvent.list() {
        if (message.contentToString() != PluginData.commandMap[CommandName.LIST]) return
        val msg = if (serverMap.isEmpty()) {
            "无服务器列表..."
        } else {
            var names = ""
            for ((name, address) in serverMap) {
                names += "[$name]${address.address}:${address.port}\n"
            }
            "服务器列表为:\n$names"
        }
        launch { group.sendMessage(msg) }
    }

    /**
     * ping 所有服务器
     *
     */
    @EventHandler
    fun GroupMessageEvent.pingAll() {
        if (message.contentToString() != PluginData.commandMap[CommandName.PING_ALL]) return
        if (PluginData.isAllToImg) {
            var imgMessage = ""
            serverMap.forEach {
                imgMessage += pingServer(it.value.address, it.value.port, it.key)
                imgMessage += "\n\n\n"
            }
            launch {
                val img = group.uploadImage(imgMessage.trimEnd().toImage().toInput(), "png")
                group.sendMessage(img)
            }
        } else {
            val message = buildForwardMessage {
                serverMap.forEach {
                    bot says (pingServer(it.value.address, it.value.port, it.key))
                }
            }
            launch {group.sendMessage(message)}
        }
    }

    @EventHandler
    fun GroupMessageEvent.pingAddress(){
        val content = message.contentToString()
        val match = """^!ping\s?(.*)\s([0-9]*)""".toRegex().find(content) ?: return
        val (address,port) = match.destructured
        launch {group.sendMessage(pingServer(address, port.toInt(), address))}
    }

    @EventHandler
    fun GroupMessageEvent.ping() {
        val content = message.contentToString()
        val match = """^${PluginData.commandMap[CommandName.PING]}\s?(.*)""".toRegex().find(content) ?: return
        val (name) = match.destructured
        serverMap[name]?.run {
            launch { group.sendMessage(pingServer(address, port, name) ) }
        }
    }

    /**
     * ping 服务器
     *
     * @param address 地址
     * @param port 端口
     * @param name 昵称
     * @return
     */
    private fun pingServer(address: String, port: Int, name: String): Message {
        return try {
            val (delay, serverStatus) = ping(address, port)
            serverStatus.toMessage(name, delay)
        } catch (e: Exception) {
            Minecraft.logger.error(e.stackTraceToString())
            return PlainText("获取服务器状态失败：${e.message}")
        }
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

        var serverList = ""
        serverMap.forEach { serverList += "[${it.key}] " }

        return PlainText(
            "服务器信息如下:\n" +
                    "名   称: $name\n" +
                    "延   迟: $delay ms\n" +
                    "版   本: ${versionName}\n" +
                    "描   述: ${descriptionColourHandle(description)}\n" +
                    "在线人数: ${playerInfo.playerOnline}/${playerInfo.playerMax}\n" +
                    "$sampleName\n" +
                    "mod个数: ${forgeData.mods.size}\n" +
                    "服务器列表:$serverList"
        )
    }

    /**
     * 服务器描述颜色处理
     */
    private fun descriptionColourHandle(description: String): String {
        return description.replace("""§[\da-z]""".toRegex(), "")
    }
}