package top.limbang.mirai.minecraft.utils


import net.mamoe.mirai.contact.Group
import top.limbang.doctor.client.MinecraftClient
import top.limbang.doctor.client.event.ChatEvent
import top.limbang.doctor.client.event.JoinGameEvent
import top.limbang.doctor.client.running.TpsEntity
import top.limbang.doctor.client.running.TpsUtils
import top.limbang.doctor.client.utils.substringBetween
import top.limbang.doctor.network.handler.onPacket
import top.limbang.doctor.protocol.definition.play.client.PlayerPositionAndLookPacket
import top.limbang.mirai.minecraft.MinecraftPluginData
import top.limbang.mirai.minecraft.MiraiConsoleMinecraftPlugin
import java.util.*

object TPSUtils {

    fun getTps(group: Group, mgs: String) {
        val serverInfo = MinecraftPluginData.serverMap[mgs] ?: return

        val decode = Base64.getDecoder()
        val password = String(decode.decode(serverInfo.loginInfo.password))
        val client = MinecraftClient()
            .user(serverInfo.loginInfo.username, password)
            .authServerUrl(serverInfo.loginInfo.authServerUrl)
            .sessionServerUrl(serverInfo.loginInfo.sessionServerUrl)
            .start(serverInfo.address, serverInfo.port)

        val tpsList = mutableListOf<TpsEntity>()

        client.on(ChatEvent) {
            if (it.chatPacket.json.contains("commands.forge.tps.summary")) {
                val tpsEntity = TpsUtils.parseTpsEntity(it.chatPacket.json)
                tpsList.add(tpsEntity)
                if (tpsEntity.dim != "Overall") return@on

                var outMsg = "[$mgs]低于20TPS的维度如下:\n"
                tpsList.filterIndexed { index, tpsEntity ->
                    val dim = tpsEntity.dim.substringBetween("Dim", "(").trim()
                    outMsg += when {
                        index == tpsList.size - 1 -> {
                            "\n全局TPS:${tpsEntity.tps} Tick时间:${tpsEntity.tickTime}"
                        }
                        tpsEntity.tps < 20 -> "TPS:%-4.4s 维度:%s\n".format(tpsEntity.tps, dim)
                        else -> ""
                    }
                    true
                }
                MiraiConsoleMinecraftPlugin.sendMessage(group, outMsg)
                client.stop()
            }
        }.once(JoinGameEvent) {
            MiraiConsoleMinecraftPlugin.sendMessage(group, "登陆成功，开始发送 forge tps 指令")
        }.onPacket<PlayerPositionAndLookPacket> {
            client.sendMessage("/forge tps")
        }

    }


}