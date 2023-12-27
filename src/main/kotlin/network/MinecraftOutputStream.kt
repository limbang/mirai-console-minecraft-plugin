/*
 * Copyright (c) 2023 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.minecraft.network

import java.io.DataOutput
import java.io.DataOutputStream

/**
 * ## 用于将 Minecraft 协议相关数据写入 DataOutputStream 的自定义输出流。
 *
 * @property output 内部使用的 DataOutputStream 对象
 */
class MinecraftOutputStream(private val output: DataOutputStream) : DataOutput by output {

    /**
     * ## 将整数值编码成 VarInt，并写入到 DataOutputStream 中。
     *
     * VarInt 是一种用于表示不定长度整数的压缩格式，用于减少数据传输时的字节长度。
     *
     * VarInt 使用了由低到高的每7位来存储整数值的不同部分，最高位表示是否还有后续字节。
     *
     * @param value 待编码的整数值。
     */
    fun writeVarInt(value: Int) {
        var valueCopy = value
        do {
            var temp = valueCopy and 127
            valueCopy = valueCopy ushr 7
            if (valueCopy != 0) temp = temp or 128
            writeByte(temp)
        } while (valueCopy != 0)
    }

    /**
     * ## 写入 UTF-8 编码字符串到 DataOutputStream 中。
     *
     * @param value 待写入的 UTF-8 编码字符串。
     */
    override fun writeUTF(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeVarInt(bytes.size)
        write(bytes)
    }

    fun size() = output.size()
}