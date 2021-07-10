package top.limbang.mirai.minecraft.extension

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * ### ByteArrayOutputStream 转 ByteArrayInputStream
 *
 */
fun ByteArrayOutputStream.toInput(): ByteArrayInputStream {
    return ByteArrayInputStream(this.toByteArray())
}