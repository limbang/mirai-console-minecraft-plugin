package top.limbang.entity

/**
 * ### 服务器信息
 *
 * - [description] 服务器描述
 * - [playerMax] 玩家最大人数
 * - [playerOnline] 玩家在线人数
 * - [playerNameList] 在线玩家名称列表
 * - [versionName] 版本名称
 * - [versionNumber] 版本号
 * - [modNumber] 模组数
 */
data class ServiceInfo(
    val description : String = "",
    val playerMax : Int = 20,
    val playerOnline : Int = 0,
    val playerNameList : List<String> = listOf(),
    val versionName : String = "",
    val versionNumber : Int = 0,
    val modNumber : Int = 0,
)
