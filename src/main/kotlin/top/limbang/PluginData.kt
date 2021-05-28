package top.limbang

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

/**
 * ### 服务器信息
 *
 * - [address] 服务器地址
 * - [port] 服务器端口
 */
@Serializable
data class ServerAddress(val address: String, val port: Int)



/**
 * ### 插件配置
 */
object PluginData : AutoSavePluginData("minecraft") {
    var serverMap: MutableMap<String, ServerAddress> by value()
}
