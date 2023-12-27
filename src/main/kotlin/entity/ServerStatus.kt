/*
 * Copyright (c) 2023 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.minecraft.entity


/**
 * ### 服务器状态信息
 *
 * - [favicon] 服务器图标
 * - [description] 服务器描述
 * - [playerInfo] 玩家信息
 * - [versionName] 版本名称
 * - [versionNumber] 版本号
 * - [forgeData] Forge相关信息
 */
data class ServerStatus(
    val favicon: String,
    val description: String,
    val playerInfo: PlayerInfo,
    val versionName: String,
    val versionNumber: Int,
    val forgeData: ForgeData
)

/**
 * 玩家信息
 *
 * @property playerMax 最大玩家数
 * @property playerOnline 当前在线玩家数
 * @property players 玩家列表
 */
data class PlayerInfo(val playerMax: Int, val playerOnline: Int, val players: List<Player>)

/**
 * 玩家
 *
 * @property name 玩家名称
 * @property id 玩家ID
 */
data class Player(val name: String, val id: String)

/**
 * 模组信息
 *
 * @property id 模组ID
 * @property version 模组版本
 */
data class Mod(val id: String, val version: String)

/**
 * 通道信息
 *
 * @property name 通道名称
 * @property version 通道版本
 * @property requiredOnClient 是否在客户端必需
 */
data class Channel(val name: String, val version: String, val requiredOnClient: Boolean)

/**
 * Forge相关信息
 *
 * @property fmlNetworkVersion FML网络版本
 * @property truncated 是否被截断
 * @property mods 模组列表
 * @property channels 通道列表
 * @constructor Create empty Forge data
 */
data class ForgeData(
    val fmlNetworkVersion: Int,
    val truncated: Boolean,
    val mods: List<Mod>,
    val channels: List<Channel>
)
