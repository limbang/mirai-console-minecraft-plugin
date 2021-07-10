package top.limbang.mirai.minecraft

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

/**
 * ### 服务器信息
 *
 * - [address] 服务器地址
 * - [port] 服务器端口
 * - [loginInfo] 登陆信息
 */
@Serializable
data class ServerAddress(val address: String, val port: Int, val loginInfo: LoginInfo?)


/**
 * ### 登陆信息
 *
 * - [authServerUrl] 验证地址
 * - [sessionServerUrl] 会话地址
 * - [username] 用户名
 * - [password] 密码 base64 加密过的
 */
@Serializable
data class LoginInfo(
    val authServerUrl: String,
    val sessionServerUrl: String,
    val username: String,
    val password: String
)

/**
 * ### 插件配置
 */
object MinecraftPluginData : AutoSavePluginData("minecraft") {
    var serverMap: MutableMap<String, ServerAddress> by value()
    var loginMap: MutableMap<String, LoginInfo> by value()
}
