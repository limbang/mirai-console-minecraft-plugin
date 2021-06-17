package top.limbang.mirai.minecraft.utils


import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import top.limbang.doctor.client.MinecraftClient
import top.limbang.doctor.client.running.AutoVersionForgePlugin
import top.limbang.doctor.client.running.TpsPlugin
import top.limbang.doctor.client.running.tpsTools
import top.limbang.doctor.client.utils.substringBetween
import top.limbang.doctor.network.handler.onPacket
import top.limbang.doctor.network.handler.oncePacket
import top.limbang.doctor.protocol.definition.play.client.JoinGamePacket
import top.limbang.doctor.protocol.definition.play.client.PlayerPositionAndLookPacket
import top.limbang.mirai.minecraft.MinecraftPluginData
import top.limbang.mirai.minecraft.MiraiConsoleMinecraftPlugin
import java.util.*

object TPSUtils {

    suspend fun getTps(group: Group, mgs: String, sender: Member) {
        val serverInfo = MinecraftPluginData.serverMap[mgs] ?: return

        val decode = Base64.getDecoder()
        val password = String(decode.decode(serverInfo.loginInfo.password))
        val client = MinecraftClient.builder()
            .user(serverInfo.loginInfo.username, password)
            .authServerUrl(serverInfo.loginInfo.authServerUrl)
            .sessionServerUrl(serverInfo.loginInfo.sessionServerUrl)
            .plugin(TpsPlugin())
            .plugin(AutoVersionForgePlugin())
            .build()

        if (!client.start(serverInfo.address, serverInfo.port,3000)) {
            sendErrorImage(group, "${sender.nameCard},好像获取失败了哦！！！")
            return
        }

        client.oncePacket<JoinGamePacket> {
            MiraiConsoleMinecraftPlugin.sendMessage(group, "登陆[$mgs]成功，开始发送 forge tps 指令")
        }.onPacket<PlayerPositionAndLookPacket> {
            GlobalScope.launch {
                val forgeTps = client.tpsTools.getTpsSuspend()
                var outMsg = "[$mgs]低于20TPS的维度如下:\n"
                forgeTps.forEach { tpsEntity ->
                    val dim = tpsEntity.dim.substringBetween("Dim", "(").trim()
                    outMsg += when {
                        tpsEntity.dim == "Overall" -> "\n全局TPS:${tpsEntity.tps} Tick时间:${tpsEntity.tickTime}"
                        tpsEntity.tps < 20 -> "TPS:%-4.4s 维度:%s\n".format(tpsEntity.tps, dim)
                        else -> ""
                    }
                }
                MiraiConsoleMinecraftPlugin.sendMessage(group, outMsg)
                client.stop()
            }
        }
    }

    private suspend fun sendErrorImage(group: Group, sendMgs: String) {
        val image = ImageErrorMessage.createImage(
            MiraiConsoleMinecraftPlugin.getResourceAsStream("9.jpg")!!,
            sendMgs
        )
        group.sendImage(image)
    }
}

