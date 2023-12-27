/*
 * Copyright 2020-2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/limbang/mirai-console-minecraft-plugin/blob/master/LICENSE
 */

package top.limbang.minecraft.mirai

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.event.registerTo
import top.limbang.minecraft.mirai.PluginCompositeCommand.renameServer
import top.limbang.mirai.event.GroupRenameEvent

object Minecraft : KotlinPlugin(
    JvmPluginDescription(
        id = "top.limbang.minecraft",
        name = "Minecraft",
        version = "1.2.0",
    ) {
        author("limbang")
        info("""Minecraft插件""")
        dependsOn("top.limbang.general-plugin-interface",true)
    }
) {
    /** 是否加载通用插件接口 */
    val isLoadGeneralPluginInterface: Boolean = try {
        Class.forName("top.limbang.mirai.GeneralPluginInterface")
        true
    } catch (e: Exception) {
        logger.info("未加载通用插件接口,limbang插件系列改名无法同步.")
        logger.info("前往 https://github.com/limbang/mirai-plugin-general-interface/releases 下载")
        false
    }

    override fun onDisable() {
        PluginCompositeCommand.unregister()
    }

    override fun onEnable() {
        PluginData.reload()
        PluginCompositeCommand.register()
        val ping = PluginData.commandMap[CommandName.PING] ?: "!"
        val list = PluginData.commandMap[CommandName.LIST] ?: "!list"
        val pingAll = PluginData.commandMap[CommandName.PING_ALL] ?: "!all"

        // 创建事件通道
        val eventChannel = GlobalEventChannel.parentScope(this)

        MinecraftListener.registerTo(eventChannel)

        eventChannel.subscribeAlways<NudgeEvent> {
            if (target.id == bot.id) {
                subject.sendMessage(
                    "Minecraft 插件使用说明:\n" +
                            "Ping服务器:$ping 服务器名称\n" +
                            "Ping服务器:!ping <地址> [端口]\n" +
                            "Ping所有服务器:$pingAll\n" +
                            "查看服务器列表:$list"
                )
            }
        }
        if (!isLoadGeneralPluginInterface) return
        // 监听改名事件
        eventChannel.subscribeAlways<GroupRenameEvent> {
            logger.info("GroupRenameEvent: pluginId = $pluginId oldName = $oldName groupId=$groupId newName = $newName")
            if (pluginId == Minecraft.id) return@subscribeAlways
            renameServer(oldName, newName,groupId,true)
        }
    }


}

