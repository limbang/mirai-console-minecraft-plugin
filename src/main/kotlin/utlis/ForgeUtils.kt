/*
 * Copyright (c) 2023 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.minecraft.utlis

import kotlinx.serialization.json.*
import top.limbang.minecraft.entity.Channel
import top.limbang.minecraft.entity.ForgeData
import top.limbang.minecraft.entity.Mod
import top.limbang.minecraft.network.MinecraftInputStream
import top.limbang.minecraft.network.MinecraftOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream


/**
 * 将经过优化的字符串解码为字节数组。
 *
 * @param s 经过优化的字符串
 * @return 解码后的字节数组
 */
private fun decodeOptimized(s: String): ByteArray {
    val byteArray = ByteArrayOutputStream()
    val output = MinecraftOutputStream(DataOutputStream(byteArray))
    val size0 = s[0].code
    val size1 = s[1].code
    val size = size0 or (size1 shl 15)

    var stringIndex = 2
    var buffer = 0 // 我们最多需要 8 + 14 = 22 位的缓冲区，因此使用 int 类型足够
    var bitsInBuf = 0
    while (stringIndex < s.length) {
        while (bitsInBuf >= 8) {
            output.writeByte(buffer)
            buffer = buffer ushr 8
            bitsInBuf -= 8
        }
        val c = s[stringIndex]
        buffer = buffer or (c.code and 0x7FFF shl bitsInBuf)
        bitsInBuf += 15
        stringIndex++
    }

    // 写入剩余的数据
    while (output.size() < size) {
        output.writeByte(buffer)
        buffer = buffer ushr 8
        bitsInBuf -= 8
    }

    return byteArray.toByteArray()
}

/**
 * 忽略服务器端标记的版本标志
 */
private const val VERSION_FLAG_IGNORE_SERVER_ONLY = 0b1
private const val IGNORE_SERVER_ONLY = "SERVER_ONLY"

/**
 * 将解码后的字节数组反序列化为 ForgeData 对象。
 *
 * @param fmlNetworkVersion Forge 网络版本号
 * @param decodedData 解码后的字节数组
 * @return 反序列化后的 ForgeData 对象
 */
private fun deserializeOptimized(fmlNetworkVersion: Int, decodedData: ByteArray): ForgeData {
    val input = MinecraftInputStream(DataInputStream(ByteArrayInputStream(decodedData)))
    val truncated = input.readBoolean()
    val mods = mutableListOf<Mod>()
    val channels = mutableListOf<Channel>()
    val modsSize = input.readUnsignedShort()
    // 解析 mod 的id和版本
    for (i in 0..<modsSize) {
        val channelSizeAndVersionFlag = input.readVarInt()
        val channelSize = channelSizeAndVersionFlag ushr 1
        val isIgnoreServerOnly = channelSizeAndVersionFlag and VERSION_FLAG_IGNORE_SERVER_ONLY != 0
        val modId = input.readUTF()
        val modVersion = if (isIgnoreServerOnly) IGNORE_SERVER_ONLY else input.readUTF()
        // 解析 mod 的通道名称、版本和客户端是否需要
        for (j in 0..<channelSize) {
            val channelName = input.readUTF()
            val channelVersion = input.readUTF()
            val requiredOnClient = input.readBoolean()
            channels.add(Channel("$modId:$channelName", channelVersion, requiredOnClient))
        }
        mods.add(Mod(modId, modVersion))
    }
    // 解析非mod的通道
    val nonModChannelCount = input.readVarInt()
    for (i in 0..<nonModChannelCount) {
        val channelName = input.readUTF(32767)
        val channelVersion = input.readUTF()
        val requiredOnClient = input.readBoolean()
        channels.add(Channel(channelName, channelVersion, requiredOnClient))
    }
    return ForgeData(fmlNetworkVersion = fmlNetworkVersion, truncated = truncated, mods = mods, channels = channels)
}

/**
 * 根据 JSON 元素和版本号获取 ForgeData 对象。
 *
 * @param element JSON 元素
 * @param versionNumber 版本号
 * @return 对应的 [ForgeData] 对象
 */
internal fun getForgeDate(element: JsonElement, versionNumber: Int): ForgeData {
    val forgeDataObject = element.jsonObject["forgeData"]
    val fmlNetworkVersion = when {
        forgeDataObject != null && versionNumber > 383 -> forgeDataObject.jsonObject["fmlNetworkVersion"]!!.jsonPrimitive.int
        element.jsonObject["modinfo"] != null && versionNumber <= 340 -> 1
        else -> 0
    }
    return when (fmlNetworkVersion) {
        3 -> forgeNetwork3(forgeDataObject!!)
        2 -> forgeNetwork2(forgeDataObject!!)
        1 -> forgeNetwork1(element.jsonObject["modinfo"]!!)
        else -> ForgeData(-1, false, listOf(), listOf())
    }
}

/**
 * 解析 fmlNetworkVersion 为 3 的 ForgeData 对象。
 * - 1.18.x - 1.20.2 forge 使用的此方法
 *
 * @param forgeDataObject fmlNetworkVersion 为 3 的 ForgeData JSON 元素
 * @return 对应的 [ForgeData] 对象
 */
private fun forgeNetwork3(forgeDataObject: JsonElement): ForgeData {
    val decodedData = forgeDataObject.jsonObject["d"]!!.jsonPrimitive.content
    return deserializeOptimized(3, decodeOptimized(decodedData))
}

/**
 * 解析 fmlNetworkVersion 为 2 的 ForgeData 对象。
 * - 1.16.x - 1.17,x forge 使用的此方法
 * - 1.13.x - 1.15.x 兼容
 *
 * @param forgeDataObject fmlNetworkVersion 为 2 的 ForgeData JSON 元素
 * @return 对应的 [ForgeData] 对象
 */
private fun forgeNetwork2(forgeDataObject: JsonElement): ForgeData {
    val channels = forgeDataObject.jsonObject["channels"]!!.jsonArray.map {
        val res = it.jsonObject["res"]!!.jsonPrimitive.content
        val version = it.jsonObject["version"]!!.jsonPrimitive.content
        val required = it.jsonObject["required"]!!.jsonPrimitive.boolean
        Channel(name = res, version = version, requiredOnClient = required)
    }

    val mods = forgeDataObject.jsonObject["mods"]!!.jsonArray.map {
        val modId = it.jsonObject["modId"]!!.jsonPrimitive.content
        val modmarker = it.jsonObject["modmarker"]!!.jsonPrimitive.content
        Mod(id = modId, version = modmarker)
    }

    // 兼容 1.13.x - 1.15.x 找不到就默认 false
    val truncated = forgeDataObject.jsonObject["truncated"]?.jsonPrimitive?.boolean ?: false
    return ForgeData(fmlNetworkVersion = 2, truncated = truncated, mods = mods, channels = channels)
}

/**
 * 解析 fmlNetworkVersion 为 1 的 ForgeData 对象。
 *
 * - 1.8.8 - 1.12.x forge 使用的此方法
 * - 1.7.10 也兼容
 *
 * @param forgeDataObject fmlNetworkVersion 为 1 的 ForgeData JSON 元素
 * @return 对应的 [ForgeData] 对象
 */
private fun forgeNetwork1(forgeDataObject: JsonElement): ForgeData {
    val mods = forgeDataObject.jsonObject["modList"]!!.jsonArray.map {
        val modId = it.jsonObject["modid"]!!.jsonPrimitive.content
        val version = it.jsonObject["version"]!!.jsonPrimitive.content
        Mod(id = modId, version = version)
    }
    return ForgeData(fmlNetworkVersion = 2, truncated = false, mods = mods, channels = listOf())
}