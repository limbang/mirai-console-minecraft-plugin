/*
 * Copyright (c) 2023 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.minecraft.network

import java.io.DataInput
import java.io.DataInputStream
import java.io.IOException

/**
 * ## 用于从 DataInputStream 中读取 Minecraft 协议相关数据的自定义输入流。
 *
 * @property input 内部使用的 DataInputStream 对象
 */
class MinecraftInputStream(private val input: DataInputStream) : DataInput by input {

    /**
     * ## 从 DataInputStream 中读取 Minecraft VarInt。
     *
     * VarInt 是一种用于表示不定长度整数的压缩格式，用于减少数据传输时的字节长度。
     *
     * VarInt 使用了由低到高的每7位来存储整数值的不同部分，最高位表示是否还有后续字节。
     *
     * @return 读取到的 VarInt 值。
     * @throws RuntimeException 如果 VarInt 超出规定的最大长度（超过5个字节）。
     */
    fun readVarInt(): Int {
        var result = 0
        var numRead = 0
        var read: Byte
        do {
            read = readByte()
            result = result or (read.toInt() and 127 shl numRead++ * 7)
            if (numRead > 5) throw RuntimeException("VarInt too big")
        } while (read.toInt() and 128 == 128)
        return result
    }

    /**
     * ## 从 DataInputStream 中读取一个 UTF-8 编码字符串。
     *
     * 默认情况下，该方法读取的字符串长度受到 Short.MAX_VALUE 的限制。
     *
     * @return 读取到的 UTF-8 编码字符串。
     */
    override fun readUTF(): String {
        return readUTF(Short.MAX_VALUE.toInt())
    }

    /**
     * ## 从 DataInputStream 中读取一个 UTF-8 编码字符串。
     *
     * @param maxLength 最大允许的字符串长度。
     * @return 读取到的 UTF-8 编码字符串。
     * @throws IOException 如果读取到的字符串长度无效。
     */
    fun readUTF(maxLength: Int): String {
        val length = readVarInt()
        if (length < 0 || length > maxLength) throw IOException("Invalid string length.")
        val bytes = ByteArray(length)
        readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}