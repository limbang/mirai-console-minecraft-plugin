/*
 * Copyright 2020-2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/limbang/mirai-console-minecraft-plugin/blob/master/LICENSE
 */

package top.limbang.minecraft.mirai

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

/**
 * ### 服务器信息
 *
 * - [address] 服务器地址
 * - [port] 服务器端口
 */
@Serializable
data class ServerAddress(val address: String, val port: Int)


enum class CommandName{
    PING,LIST,PING_ALL
}

/**
 * ### 插件配置
 */
object PluginData : AutoSavePluginData("minecraft") {
    var serverMap: MutableMap<String, ServerAddress> by value()
    var commandMap: MutableMap<CommandName, String> by value()

    @ValueDescription("All消息是否是图片,默认 false")
    var isAllToImg: Boolean by value(false)
}
