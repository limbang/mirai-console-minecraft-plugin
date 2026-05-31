/*
 * Copyright (c) 2023 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.minecraft

import kotlinx.serialization.json.*
import top.limbang.minecraft.entity.Player
import top.limbang.minecraft.entity.PlayerInfo
import top.limbang.minecraft.entity.ServerStatus
import top.limbang.minecraft.network.MinecraftInputStream
import top.limbang.minecraft.network.MinecraftOutputStream
import top.limbang.minecraft.utlis.getForgeDate
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket


fun ping(host: String, port: Int) = MinecraftClient.ping(host, port)

object MinecraftClient {

    /**
     * Ping
     *
     * @param host 地址
     * @param port 端口
     * @return 延迟 和 服务器状态
     */
    fun ping(host: String, port: Int): Pair<Int, ServerStatus> {
        Socket().use { socket ->
            // 设置连接超时时间
            socket.connect(InetSocketAddress(host, port), 100)
            // 设置读取超时时间
            socket.soTimeout = 1500

            val inStream = MinecraftInputStream(DataInputStream(socket.getInputStream()))
            val outStream = MinecraftOutputStream(DataOutputStream(socket.getOutputStream()))

            // 发送握手包
            val handshake = encodeHandshake(address = host, port = port)
            outStream.sendPacket(handshake)
            // 发送状态请求数据包
            outStream.writeVarInt(0x01)
            outStream.writeByte(0x00)

            // 读取握手包响应
            val jsonString = decodeHandshakeResponse(inStream)
            // 获取ping延时
            val delay = getPingDelay(inStream, outStream)
            val serverStatus = handleServerStatusJson(jsonString)
            //decodeFavicon(serverStatus.favicon)
            return Pair(delay,serverStatus)
        }
    }

    /**
     * 处理服务器状态JSON
     *
     * @param json
     * @return
     */
    fun handleServerStatusJson(json: String): ServerStatus {
        val element = Json.parseToJsonElement(json)
        // 解析版本信息
        val versionObject = element.jsonObject["version"] ?: throw RuntimeException("未找到版本信息...")
        val versionName = versionObject.jsonObject["name"]!!.jsonPrimitive.content
        val versionNumber = versionObject.jsonObject["protocol"]!!.jsonPrimitive.int
        // 解析玩家信息
        val playerObject = element.jsonObject["players"]!!
        val playerMax = playerObject.jsonObject["max"]!!.jsonPrimitive.int
        val playerOnline = playerObject.jsonObject["online"]!!.jsonPrimitive.int
        val players = mutableListOf<Player>()
        playerObject.jsonObject["sample"]?.jsonArray?.forEach {
            players.add(
                Player(
                    id = it.jsonObject["id"]!!.jsonPrimitive.content,
                    name = it.jsonObject["name"]!!.jsonPrimitive.content
                )
            )
        }
        val playerInfo = PlayerInfo(playerMax = playerMax, playerOnline = playerOnline, players = players)
        // 解析服务器头像
        val favicon = element.jsonObject["favicon"]?.jsonPrimitive?.content ?: ""
        // 解析描述
        val description = getDescription(element.jsonObject["description"]!!)

        return ServerStatus(
            favicon = favicon,
            description = description,
            playerInfo = playerInfo,
            versionName = versionName,
            versionNumber = versionNumber,
            forgeData = getForgeDate(element, versionNumber)
        )
    }

    /**
     * 获取服务器描述
     *
     */
    private fun getDescription(element: JsonElement): String {
        // 1.7.10  及以下 描述直接在 description
        if (element is JsonPrimitive) return element.jsonPrimitive.content
        // cleanroom 描述在 description.translate
        if (element.jsonObject.containsKey("translate")) return element.jsonObject["translate"]!!.jsonPrimitive.content
        // forge 描述在 description.text
        if (element.jsonObject.containsKey("text")) return element.jsonObject["text"]!!.jsonPrimitive.content
        return "未解析成功，请联系作者"
    }


    /**
     * 根据 minecraft 协议发送数据包
     *
     */
    private fun MinecraftOutputStream.sendPacket(packet: ByteArray) {
        writeVarInt(packet.size)
        write(packet)
    }

    /**
     * 根据 minecraft 协议编码握手包
     *
     * @param version 服务器版本 默认为：-1
     * @param address 服务器地址
     * @param port 服务器端口
     * @param state 下一步状态 1：状态 2：登录
     * @return
     */
    private fun encodeHandshake(version: Int = -1, address: String, port: Int, state: Int = 1): ByteArray {
        val byte = ByteArrayOutputStream()
        val handshake = MinecraftOutputStream(DataOutputStream(byte))
        handshake.writeByte(0x00)
        handshake.writeVarInt(version)
        handshake.writeUTF(address)
        handshake.writeShort(port)
        handshake.writeVarInt(state)
        return byte.toByteArray()
    }

    /**
     * 根据 minecraft 协议解码握手包状态响应
     *
     * @return 服务器状态 json 字符串
     */
    private fun decodeHandshakeResponse(inStream: MinecraftInputStream): String {
        val responseLength = inStream.readVarInt()
        val packetId = inStream.readVarInt()
        if (packetId == -1) throw IOException("Premature end of stream.")
        if (packetId != 0x00) throw IOException("Invalid packetID: $packetId")
        return inStream.readUTF(responseLength)
    }

    /**
     * 获取 ping 延迟
     *
     * @param outStream
     * @param inStream
     * @return 成功返回延迟时间（毫秒），失败返回-1
     */
    private fun getPingDelay(inStream: MinecraftInputStream, outStream: MinecraftOutputStream) = try {
        // 发送 ping 包
        val now = System.currentTimeMillis()
        outStream.writeByte(0x09)
        outStream.writeByte(0x01)
        outStream.writeLong(now)
        // 读取 ping 响应
        inStream.readVarInt()
        val id = inStream.readVarInt()
        if (id == -1) throw IOException("Premature end of stream.")
        if (id != 0x01) throw IOException("Invalid packetID")
        (System.currentTimeMillis() - inStream.readLong()).toInt()
    } catch (e: Exception) {
        println(e.localizedMessage)
        -1
    }
}





