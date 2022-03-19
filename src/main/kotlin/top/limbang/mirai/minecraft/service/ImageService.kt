/*
 * Copyright 2020-2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/limbang/mirai-console-minecraft-plugin/blob/master/LICENSE
 */

package top.limbang.mirai.minecraft.service

import top.limbang.mirai.minecraft.MiraiConsoleMinecraftPlugin
import top.limbang.mirai.minecraft.extension.addSubtitles
import top.limbang.mirai.minecraft.extension.readImage
import top.limbang.mirai.minecraft.extension.toInput
import java.io.ByteArrayInputStream

object ImageService {

    private val errorMsgList = listOf(
        "希望睡醒服务器就好了.",
        "你把服务器玩坏了!!!",
        "等我喝口奶在试试.",
        "服务器连接不上,完蛋了.",
        "你干了啥?为啥连接不上.",
        "服务器故障!服务器故障!"
    )

    /**
     * ### 创建错误图片
     * @param name 提醒人的名称
     */
    fun createErrorImage(name: String): ByteArrayInputStream {
        val randoms = (0..5).random()
        val subtitles = name + errorMsgList[randoms]
        return createSubtitlesImage(subtitles, "$randoms.jpg")
    }

    /**
     * ### 创建字幕图片
     * @param subtitles 字幕
     * @param resourceName 资源图片名称
     */
    fun createSubtitlesImage(subtitles: String, resourceName: String): ByteArrayInputStream {
        val resourceImageStream =
            MiraiConsoleMinecraftPlugin.getResourceAsStream(resourceName) ?: throw RuntimeException("读取不到资源文件.")
        val image = resourceImageStream.use { resourceImageStream -> resourceImageStream.readImage() }
        return image.addSubtitles(subtitles).toInput()
    }
}