/*
 * Copyright 2020-2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/limbang/mirai-console-minecraft-plugin/blob/master/LICENSE
 */

package top.limbang.minecraft

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.UserCommandSender
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.broadcast
import top.limbang.minecraft.PluginData.isAllToImg
import top.limbang.minecraft.PluginData.isTps
import top.limbang.minecraft.PluginData.serverMap
import top.limbang.mirai.event.GroupRenameEvent


/**
 * ### 插件指令
 */
object PluginCompositeCommand : CompositeCommand(Minecraft, "mc") {
    @SubCommand
    @Description("设置触发指令")
    suspend fun CommandSender.setCommand(name: CommandName, command: String) {
        PluginData.commandMap[name] = command
        sendMessage("配置[$name]触发指令[$command],需重启机器人生效.")
    }


    @SubCommand
    @Description("查看登陆信息")
    suspend fun CommandSender.loginInfo() {
        var loginInfo = ""
        PluginData.loginMap.forEach {
            loginInfo += "[${it.key}] "
        }
        if (loginInfo.isEmpty())
            sendMessage("无登陆信息...")
        else
            sendMessage("登陆配置信息名称:$loginInfo")
    }

    @SubCommand
    @Description("添加登陆信息")
    suspend fun CommandSender.addLogin(
        name: String, authServerUrl: String, sessionServerUrl: String, username: String, password: String
    ) {
        val group = getGroupOrNull()
        if (group != null) {
            sendMessage("此配置不能在群配置.")
            return
        }
        LoginInfo(authServerUrl, sessionServerUrl, username, password)
            .also { PluginData.loginMap[name] = it }
        sendMessage("[$name]登陆配置添加成功.")
    }

    @SubCommand
    @Description("删除登陆信息")
    suspend fun CommandSender.deleteLogin(name: String) {
        if (PluginData.loginMap.keys.remove(name)) {
            sendMessage("登陆配置[$name]删除成功.")
        } else {
            sendMessage("登陆配置[$name]删除失败.")
        }
    }

    @SubCommand
    @Description("添加服务器,端口默认 25565")
    suspend fun CommandSender.addServer(name: String, address: String, port: Int = 25565) {
        serverMap[name] = ServerAddress(address, port, null)
        sendMessage("服务器[$name]添加成功.")
    }

    @SubCommand
    @Description("添加带登陆信息带服务器,端口默认 25565")
    suspend fun CommandSender.addServerLogin(loginName: String, name: String, address: String, port: Int = 25565) {
        val loginInfo = PluginData.loginMap[loginName]
        if (loginInfo == null) {
            sendMessage("[$loginName]该登陆信息尚未配置.")
            return
        }

        serverMap[name] = ServerAddress(address, port, loginInfo)
        sendMessage("服务器[$name]添加成功.")
    }

    @SubCommand
    @Description("删除服务器")
    suspend fun CommandSender.deleteServer(name: String) {
        if (serverMap.keys.remove(name)) {
            sendMessage("服务器[$name]删除成功.")
        } else {
            sendMessage("服务器[$name]删除失败.")
        }
    }

    @SubCommand
    @Description("重新命名服务器")
    suspend fun UserCommandSender.rename(name: String, newName: String) {
        if (isNotGroup()) return
        if (renameServer(name, newName, subject.id, false)) sendMessage("原[$name]修改[$newName]成功.")
        else sendMessage("没有找到[$name]服务器.")
    }

    internal suspend fun UserCommandSender.isNotGroup() = (subject !is Group).also {
        if (it) sendMessage("请在群内发送命令")
    }

    internal suspend fun renameServer(name: String, newName: String, groupId: Long, isEvent: Boolean): Boolean {
        val server = serverMap[name]
        return if (server != null) {
            serverMap.remove(name)
            serverMap[newName] = server
            if (Minecraft.isLoadGeneralPluginInterface) {
                // 发布改名广播
                if (!isEvent) GroupRenameEvent(groupId, Minecraft.id, name, newName).broadcast()
            }
            true
        } else false
    }

    @SubCommand
    @Description("设置tps功能启用")
    suspend fun CommandSender.setTps(value: Boolean) {
        isTps = value
        sendMessage("tps功能:$isTps")
    }

    @SubCommand
    @Description("设置All消息转换为图片")
    suspend fun CommandSender.setAllToImg(value: Boolean) {
        isAllToImg = value
        sendMessage("All消息转换为图片功能:$isAllToImg")
    }
}