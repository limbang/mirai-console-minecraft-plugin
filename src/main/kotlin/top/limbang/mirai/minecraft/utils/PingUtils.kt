package top.limbang.mirai.minecraft.utils

import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import top.limbang.doctor.client.MinecraftClient
import top.limbang.doctor.client.entity.ServerInfo
import top.limbang.doctor.client.utils.ServerInfoUtils
import top.limbang.mirai.minecraft.MinecraftPluginData
import top.limbang.mirai.minecraft.MiraiConsoleMinecraftPlugin
import java.util.concurrent.TimeUnit

object PingUtils {
    private val errorMsgList = listOf(
        "希望睡醒服务器就好了.",
        "你把服务器玩坏了!!!",
        "等我喝口奶在试试.",
        "服务器连接不上,完蛋了.",
        "你干了啥?为啥连接不上.",
        "服务器故障!服务器故障!"
    )

    suspend fun pingServer(group: Group, mgs: String, sender: Member) {
        if (mgs.isEmpty()) return
        val serverInfo = MinecraftPluginData.serverMap[mgs] ?: return
        val serverListInfo: ServerInfo
        try {
            val json = MinecraftClient.ping(serverInfo.address, serverInfo.port).get(3000, TimeUnit.MILLISECONDS)
            if (json == null) {
                sendErrorImage(group, sender)
                return
            }
            serverListInfo = ServerInfoUtils.getServiceInfo(json)
        } catch (e: Exception) {
            sendErrorImage(group, sender)
            return
        }
        var sampleName = ""
        serverListInfo.playerNameList.forEach {
            sampleName += "[${it}] "
        }

        var serverList = ""
        MinecraftPluginData.serverMap.forEach {
            serverList += "[${it.key}] "
        }

        val sendMgs = "服务器信息如下:\n" +
                "名   称: $mgs\n" +
                "版   本: ${serverListInfo.versionName}\n" +
                "描   述: ${serverListInfo.description}\n" +
                "在线人数: ${serverListInfo.playerOnline}/${serverListInfo.playerMax}\n" +
                "$sampleName\n" +
                "mod个数: ${serverListInfo.modNumber}\n" +
                "服务器列表:$serverList"

        group.sendMessage(At(sender).plus(sendMgs))
    }

    private suspend fun sendErrorImage(group: Group, sender: Member) {
        val randoms = (0..5).random()
        val sendMgs = sender.nameCard + errorMsgList[randoms]
        val image =
            ImageErrorMessage.createImage(
                MiraiConsoleMinecraftPlugin.getResourceAsStream("$randoms.jpg")!!,
                sendMgs
            )
        group.sendImage(image)

        val adminList = MinecraftPluginData.adminMap[group.id] ?: return
        group.sendMessage(buildMessageChain {
            +PlainText("大召唤术~~ (╬▔皿▔)╯")
            adminList.forEach {
                add(At(it))
            }
        })
    }
}