/*
 * Copyright 2026 limbang and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/limbang/mirai-console-minecraft-plugin/blob/master/LICENSE
 */

package top.limbang.minecraft.utlis

import top.limbang.minecraft.MinecraftClient
import top.limbang.minecraft.entity.Player
import top.limbang.minecraft.entity.ServerStatus
import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.random.Random

/**
 * 服务器状态图片生成器。
 *
 * 该对象负责把 ping 返回的 [ServerStatus] 渲染为单一科技风 PNG 图片流，
 * 包括：
 * - 主信息区：服务器名称、描述、延迟、在线人数、模组提示、版本信息
 * - 玩家列表区：最多展示 12 个玩家名称
 *
 * 当前实现只保留一种科技风视觉方案，不再维护多主题分支。
 */
object ServerStatusImageGenerator {

    private const val WIDTH = 760
    private const val FAILURE_HEIGHT = 238
    private const val MAX_PLAYERS = 12
    private const val CARD_PADDING = 24
    private const val SECTION_GAP = 16
    private const val DESCRIPTION_WIDTH = 490
    private const val DESCRIPTION_LINE_HEIGHT = 20
    private const val HERO_MIN_HEIGHT = 214
    private const val HERO_ICON_SIZE = 88
    private const val PLAYER_CHIP_HEIGHT = 30
    private const val PLAYER_CHIP_PADDING_X = 14
    private const val PLAYER_GAP_X = 10
    private const val PLAYER_GAP_Y = 12
    private const val PLAYER_LINE_HEIGHT = PLAYER_CHIP_HEIGHT + PLAYER_GAP_Y
    private const val PLAYER_SECTION_START_Y = 102
    private const val PLAYER_BOTTOM_PADDING = 14
    private const val DEFAULT_HEIGHT = 340
    private const val OVERVIEW_GAP = 0
    private const val OVERVIEW_FOOTER_HEIGHT = 42

    private val titleFont = Font("Dialog", Font.BOLD, 28)
    private val bodyFont = Font("Dialog", Font.PLAIN, 15)
    private val sectionFont = Font("Dialog", Font.BOLD, 16)
    private val labelFont = Font("Dialog", Font.PLAIN, 13)
    private val chipFont = Font("Dialog", Font.PLAIN, 14)
    private val pingFont = Font("Dialog", Font.BOLD, 20)
    private val footerFont = Font("Dialog", Font.PLAIN, 13)
    private val overviewTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val mcColors = mapOf(
        '0' to Color(0, 0, 0),
        '1' to Color(0, 0, 170),
        '2' to Color(0, 170, 0),
        '3' to Color(0, 170, 170),
        '4' to Color(170, 0, 0),
        '5' to Color(170, 0, 170),
        '6' to Color(255, 170, 0),
        '7' to Color(170, 170, 170),
        '8' to Color(85, 85, 85),
        '9' to Color(85, 85, 255),
        'a' to Color(85, 255, 85),
        'b' to Color(85, 255, 255),
        'c' to Color(255, 85, 85),
        'd' to Color(255, 85, 255),
        'e' to Color(255, 255, 85),
        'f' to Color.WHITE
    )

    private val style = TechStyle(
        backgroundTop = Color(12, 22, 42),
        backgroundBottom = Color(4, 10, 24),
        panelOuter = Color(44, 93, 164, 70),
        panelInner = Color(8, 18, 34, 126),
        panelStroke = Color(99, 211, 255, 76),
        panelGlow = Color(255, 255, 255, 16),
        badgeFill = Color(18, 48, 76, 210),
        badgeBorder = Color(94, 222, 255, 120),
        metaFill = Color(14, 30, 52, 210),
        metaBorder = Color(75, 166, 224, 110),
        iconStroke = Color(110, 224, 255, 100),
        chipFill = Color(16, 34, 60),
        chipBorder = Color(87, 184, 255, 80),
        divider = Color(74, 165, 222),
        primaryText = Color(240, 248, 255),
        secondaryText = Color(165, 195, 219),
        highlightText = Color(93, 229, 255),
        shadowColor = Color(0, 7, 18, 190),
        shadowOffset = 2,
        signalOff = Color(74, 95, 124)
    )

    /**
     * 用于批量 ping 的服务器目标信息。
     *
     * @property serverName 图片中展示的服务器名称
     * @property host 服务器地址
     * @property port 服务器端口
     */
    data class PingTarget(val serverName: String, val host: String, val port: Int)

    /**
     * 直接发起一次 ping，并把返回结果渲染成服务器状态图片字节流。
     *
     * @param serverName 展示用服务器名称
     * @param host 服务器地址
     * @param port 服务器端口
     * @return PNG 图片输出流
     */
    fun generateFromPing(serverName: String, host: String, port: Int): ByteArrayOutputStream {
        return try {
            val (delay, status) = MinecraftClient.ping(host, port)
            generate(serverName, delay, status)
        } catch (exception: Exception) {
            generateConnectionFailed(serverName, host, port, exception.message)
        }
    }

    /**
     * 为连接失败场景生成一张专用状态图片。
     *
     * 当网络不可达、连接被拒绝或 ping 过程抛出异常时，
     * 可以使用该方法稳定输出一张失败卡片，而不是把异常继续向上抛出。
     *
     * @param serverName 展示用服务器名称
     * @param host 服务器地址
     * @param port 服务器端口
     * @param errorMessage 可选的错误说明
     * @return PNG 图片输出流
     */
    fun generateConnectionFailed(
        serverName: String,
        host: String,
        port: Int,
        errorMessage: String? = null
    ): ByteArrayOutputStream {
        val height = FAILURE_HEIGHT
        val image = createImage(WIDTH, height)
        val endpoint = "$host:$port"
        val normalizedError = normalizeFailureMessage(errorMessage)

        image.withGraphics { g ->
            g.applyImageQuality()
            drawBackground(g, WIDTH, height, computeFailureSeed(serverName, host, port, normalizedError))
            drawFailureSection(g, serverName, endpoint, normalizedError)
        }

        return image.toOutputStream("png")
    }

    /**
     * 对一组服务器目标逐个执行 ping，并把生成的卡片纵向合成为一张总图。
     *
     * 单个目标 ping 失败时不会中断整体流程，而是自动使用连接失败卡片占位，
     * 这样调用方始终可以拿到完整的总览图片。
     *
     * @param targets 待 ping 的服务器列表
     * @return 纵向拼接后的 PNG 总图输出流
     */
    fun generateFromPingList(targets: List<PingTarget>): ByteArrayOutputStream {
        if (targets.isEmpty()) {
            return generateConnectionFailed(
                serverName = "SERVER OVERVIEW",
                host = "-",
                port = 0,
                errorMessage = "No servers configured"
            )
        }

        val cardStreams = targets.map { target ->
            generateFromPing(target.serverName, target.host, target.port)
        }
        return composeImageList(cardStreams)
    }

    /**
     * 把已有的服务器状态数据渲染为 PNG 图片流。
     *
     * 该入口适合已经完成 ping 的调用方，避免重复网络请求。
     *
     * @param serverName 展示用服务器名称
     * @param delay ping 延迟，单位毫秒
     * @param status 服务器状态数据
     * @return PNG 图片输出流
     */
    fun generate(serverName: String, delay: Int, status: ServerStatus): ByteArrayOutputStream {
        val backgroundSeed = computeBackgroundSeed(serverName, status, delay)
        val visiblePlayers = status.playerInfo.players.take(MAX_PLAYERS)
        val hasVisiblePlayers = visiblePlayers.isNotEmpty()
        val measureGraphics = createMeasureGraphics(bodyFont)
        val descriptionLines = measureMinecraftTextLines(
            measureGraphics, status.description, DESCRIPTION_WIDTH
        )
        val playerRows = estimatePlayerRows(
            measureGraphics, visiblePlayers, WIDTH - CARD_PADDING * 2 - 36
        )
        measureGraphics.dispose()

        val descriptionHeight = descriptionLines * DESCRIPTION_LINE_HEIGHT
        // 主卡片保持紧凑，但需要给描述区和底部摘要标签留出稳定空间。
        val heroHeight = maxOf(HERO_MIN_HEIGHT, 112 + descriptionHeight)
        val extraPlayers = status.playerInfo.players.size - visiblePlayers.size
        val playerContentHeight =
            if (visiblePlayers.isEmpty()) 24 else (playerRows - 1) * PLAYER_LINE_HEIGHT + PLAYER_CHIP_HEIGHT
        val playerSectionHeight =
            PLAYER_SECTION_START_Y + playerContentHeight + PLAYER_BOTTOM_PADDING + if (extraPlayers > 0) 22 else 0
        val playersTop = CARD_PADDING + heroHeight + SECTION_GAP
        // 没有玩家列表时直接折叠底部区域，避免图片下半部分出现大块留白。
        val baseHeight =
            if (hasVisiblePlayers) playersTop + playerSectionHeight + CARD_PADDING else CARD_PADDING + heroHeight + CARD_PADDING
        val height = if (hasVisiblePlayers) maxOf(DEFAULT_HEIGHT, baseHeight) else baseHeight

        val image = createImage(WIDTH, height)
        image.withGraphics { g ->
            g.applyImageQuality()
            drawBackground(g, WIDTH, height, backgroundSeed)
            drawHeroSection(g, serverName, status, delay, heroHeight)
            if (hasVisiblePlayers) {
                drawPlayersSection(g, visiblePlayers, playersTop, extraPlayers)
            }
        }

        return image.toOutputStream("png")
    }

    /**
     * 绘制整张图片的科技风背景。
     *
     * 背景使用稳定随机种子生成抽象发光圆、短线和圆环，
     * 避免同一服务器每次渲染都完全不同。
     */
    private fun drawBackground(g: Graphics2D, width: Int, height: Int, seed: Int) {
        g.paint = GradientPaint(
            0f, 0f, style.backgroundTop, 0f, height.toFloat(), style.backgroundBottom
        )
        g.fillRect(0, 0, width, height)

        val random = Random(seed)
        val primaryGlowSize = 180 + random.nextInt(110)
        val primaryGlowX = -50 + random.nextInt((width * 2 / 5).coerceAtLeast(1))
        val primaryGlowY = -30 + random.nextInt((height / 3).coerceAtLeast(1))
        val secondaryGlowSize = 130 + random.nextInt(110)
        val secondaryGlowX = (width * 3 / 5) + random.nextInt((width / 4).coerceAtLeast(1))
        val secondaryGlowY = 10 + random.nextInt((height / 2).coerceAtLeast(1))

        g.color = Color(94, 222, 255, 28)
        g.fill(
            Ellipse2D.Float(
                primaryGlowX.toFloat(),
                primaryGlowY.toFloat(),
                primaryGlowSize.toFloat(),
                primaryGlowSize.toFloat()
            )
        )
        g.color = Color(85, 110, 255, 22)
        g.fill(
            Ellipse2D.Float(
                secondaryGlowX.toFloat(),
                secondaryGlowY.toFloat(),
                secondaryGlowSize.toFloat(),
                secondaryGlowSize.toFloat()
            )
        )

        // 心形只作为极轻的氛围元素加入，避免破坏当前偏科技的整体基调。
        g.stroke = BasicStroke(1.4f)
        g.color = Color(112, 222, 255, 18)
        g.draw(drawHeartShape(width - 92f, 78f, 42f, 38f))
        g.color = Color(116, 192, 255, 12)
        g.draw(drawHeartShape(106f, height - 74f, 34f, 30f))

        repeat(18) {
            val lineX = random.nextInt(width)
            val lineY = random.nextInt(height)
            val lineLength = 26 + random.nextInt(74)
            g.color = Color(115, 231, 255, 22 + random.nextInt(18))
            g.drawLine(
                lineX,
                lineY,
                (lineX + lineLength).coerceAtMost(width),
                (lineY - lineLength / 2).coerceAtLeast(0)
            )
        }
        repeat(10) {
            val ringSize = 18 + random.nextInt(48)
            val ringX = random.nextInt((width - ringSize).coerceAtLeast(1))
            val ringY = random.nextInt(height - ringSize)
            g.color = Color(126, 209, 255, 18)
            g.drawOval(ringX, ringY, ringSize, ringSize)
        }
        g.stroke = BasicStroke()
    }

    /**
     * 绘制连接失败状态的主卡片。
     */
    private fun drawFailureSection(
        g: Graphics2D,
        serverName: String,
        endpoint: String,
        errorMessage: String
    ) {
        val x = CARD_PADDING
        val y = CARD_PADDING
        val width = WIDTH - CARD_PADDING * 2
        val height = FAILURE_HEIGHT - CARD_PADDING * 2
        val failureColor = pingColor(999)
        fillPanel(g, x, y, width, height, 28)

        drawServerIcon(g, "", x + 22, y + 20, HERO_ICON_SIZE)

        val infoX = x + 132
        g.font = titleFont
        drawStyledText(g, serverName, infoX, y + 52, style.primaryText)
        drawPingInfo(g, -1, x + width - 184, y + 22, 144, 38)

        g.font = sectionFont
        drawStyledText(g, "CONNECTION FAILED", infoX, y + 96, failureColor)

        g.font = bodyFont
        drawStyledText(g, endpoint, infoX, y + 122, style.secondaryText)
        drawStyledText(g, errorMessage, infoX, y + 152, style.secondaryText)
    }

    /**
     * 绘制顶部主卡片。
     *
     * 主卡片承载最关键的信息：名称、描述、延迟和摘要标签。
     */
    private fun drawHeroSection(
        g: Graphics2D, serverName: String, status: ServerStatus, delay: Int, heroHeight: Int
    ) {
        val x = CARD_PADDING
        val y = CARD_PADDING
        val width = WIDTH - CARD_PADDING * 2
        fillPanel(g, x, y, width, heroHeight, 28)

        drawServerIcon(g, status.favicon, x + 22, y + 20, HERO_ICON_SIZE)

        val infoX = x + 132

        g.font = titleFont
        drawStyledText(g, serverName, infoX, y + 52, style.primaryText)

        drawPingInfo(g, delay, x + width - 184, y + 22, 144, 38)

        g.font = bodyFont
        // 描述是主卡片的视觉主体，需要与标题拉开距离，并给底部摘要标签留出固定空间。
        drawMinecraftTextBlock(g, status.description, infoX, y + 88, DESCRIPTION_WIDTH)

        drawHeroMetaPills(g, infoX, y + heroHeight - 56, status)
    }

    /**
     * 绘制主卡片底部的摘要标签。
     *
     * NeoForge 在 `fmlNetworkVersion == 4` 时，ping 阶段通常拿不到模组列表，
     * 因此这里不显示误导性的 `0 mods`，而是显式提示 `NeoForge ping-only`。
     */
    private fun drawHeroMetaPills(g: Graphics2D, startX: Int, y: Int, status: ServerStatus) {
        val onlineText = "${status.playerInfo.playerOnline}/${status.playerInfo.playerMax} online"
        val modsText = if (status.forgeData.fmlNetworkVersion == 4) {
            "NeoForge ping-only"
        } else {
            "${status.forgeData.mods.size} mods"
        }
        val versionText = status.versionName

        g.font = sectionFont
        val onlineWidth = g.fontMetrics.stringWidth(onlineText) + 26
        val modsWidth = g.fontMetrics.stringWidth(modsText) + 26
        val versionWidth = g.fontMetrics.stringWidth(versionText) + 26

        fillBadge(g, startX, y, onlineWidth, 30, style.metaFill, style.metaBorder)
        drawStyledText(g, onlineText, startX + 12, y + 20, style.primaryText)

        fillBadge(g, startX + onlineWidth + 12, y, modsWidth, 30, style.metaFill, style.metaBorder)
        drawStyledText(g, modsText, startX + onlineWidth + 24, y + 20, style.primaryText)

        fillBadge(g, startX + onlineWidth + modsWidth + 24, y, versionWidth, 30, style.metaFill, style.metaBorder)
        drawStyledText(g, versionText, startX + onlineWidth + modsWidth + 36, y + 20, style.primaryText)
    }

    /**
     * 绘制玩家列表区。
     *
     * 该区域只在服务器状态中实际返回 `players.sample` 时显示。
     */
    private fun drawPlayersSection(
        g: Graphics2D, visiblePlayers: List<Player>, top: Int, extraPlayers: Int
    ) {
        val rows = estimatePlayerRows(g, visiblePlayers, WIDTH - CARD_PADDING * 2 - 36)
        val contentHeight = (rows - 1) * PLAYER_LINE_HEIGHT + PLAYER_CHIP_HEIGHT
        val cardHeight =
            PLAYER_SECTION_START_Y + contentHeight + PLAYER_BOTTOM_PADDING + if (extraPlayers > 0) 22 else 0
        val x = CARD_PADDING
        val width = WIDTH - CARD_PADDING * 2

        fillPanel(g, x, top, width, cardHeight, 28)
        g.font = sectionFont
        drawStyledText(g, "ONLINE PLAYERS", x + 18, top + 30, style.primaryText)

        g.font = labelFont
        drawStyledText(g, "Showing up to 12 player names returned by the status query", x + 18, top + 50, style.secondaryText)
        g.color = style.divider
        g.fillRoundRect(x + 18, top + 62, width - 36, 4, 4, 4)

        val playerY = top + PLAYER_SECTION_START_Y
        drawPlayersFlow(g, visiblePlayers, x + 18, playerY, width - 36)

        if (extraPlayers > 0) {
            g.font = labelFont
            drawStyledText(g, "$extraPlayers more players not shown", x + 18, top + cardHeight - 14, style.highlightText)
        }
    }

    /**
     * 绘制右上角延迟信息，使延迟文本下沿与信号条底部更自然地对齐。
     */
    private fun drawPingInfo(g: Graphics2D, delay: Int, x: Int, y: Int, width: Int, height: Int) {
        val color = pingColor(delay)
        val pingText = formatPingText(delay)

        g.font = pingFont
        val metrics = g.fontMetrics
        val textWidth = metrics.stringWidth(pingText)
        val barsX = x + width - 40
        val textX = barsX - 14 - textWidth
        val barsBaseline = y + height / 2 + 9
        val textBaseline = barsBaseline + 1

        drawStyledText(g, pingText, textX, textBaseline, color)
        drawPingBars(g, delay, barsX, barsBaseline)
    }

    /**
     * 绘制统一的半透明卡片面板。
     */
    private fun fillPanel(g: Graphics2D, x: Int, y: Int, width: Int, height: Int, radius: Int) {
        g.color = style.panelOuter
        g.fillRoundRect(x, y, width, height, radius, radius)
        g.color = style.panelInner
        g.fillRoundRect(x + 2, y + 2, width - 4, height - 4, radius - 4, radius - 4)
        g.color = style.panelStroke
        g.drawRoundRect(x + 1, y + 1, width - 3, height - 3, radius - 2, radius - 2)
        g.color = style.panelGlow
        g.drawRoundRect(x + 4, y + 4, width - 9, height - 9, radius - 8, radius - 8)
    }

    /**
     * 绘制摘要信息和版本信息使用的小徽标底板。
     */
    private fun fillBadge(g: Graphics2D, x: Int, y: Int, width: Int, height: Int, fill: Color, border: Color) {
        g.color = border
        g.fillRoundRect(x, y, width, height, 16, 16)
        g.color = fill
        g.fillRoundRect(x + 2, y + 2, width - 4, height - 4, 14, 14)
    }

    /**
     * 绘制服务器图标。
     *
     * 当服务器没有返回 favicon，或者 favicon 解析失败时，自动回退到默认图标。
     */
    private fun drawServerIcon(g: Graphics2D, favicon: String, x: Int, y: Int, size: Int) {
        val icon = try {
            if (favicon.isBlank()) loadDefaultFaviconImage() else decodeFaviconImage(favicon)
        } catch (_: Exception) {
            loadDefaultFaviconImage()
        }

        g.clip = RoundRectangle2D.Float(x.toFloat(), y.toFloat(), size.toFloat(), size.toFloat(), 22f, 22f)
        g.drawImage(icon, x, y, size, size, null)
        g.clip = null
        g.color = style.iconStroke
        g.drawRoundRect(x, y, size - 1, size - 1, 22, 22)
    }

    /**
     * 按流式布局绘制玩家名称胶囊，并在宽度不足时自动换行。
     */
    private fun drawPlayersFlow(
        g: Graphics2D, players: List<Player>, startX: Int, startY: Int, maxWidth: Int
    ): Int {
        if (players.isEmpty()) return 1

        var x = startX
        var y = startY
        g.font = chipFont
        val metrics = g.fontMetrics

        players.forEach { player ->
            val chipWidth = metrics.stringWidth(player.name) + PLAYER_CHIP_PADDING_X * 2
            if (x > startX && x + chipWidth > startX + maxWidth) {
                x = startX
                y += PLAYER_LINE_HEIGHT
            }

            g.color = style.chipBorder
            g.fillRoundRect(x, y - 20, chipWidth, PLAYER_CHIP_HEIGHT, 16, 16)
            g.color = style.chipFill
            g.fillRoundRect(x + 2, y - 18, chipWidth - 4, PLAYER_CHIP_HEIGHT - 4, 14, 14)

            drawStyledText(g, player.name, x + PLAYER_CHIP_PADDING_X, y, style.primaryText)
            x += chipWidth + PLAYER_GAP_X
        }

        return ((y - startY) / PLAYER_LINE_HEIGHT) + 1
    }

    /**
     * 绘制带 Minecraft 颜色码支持的描述文本块。
     *
     * 会自动跳过 `§` 颜色控制符，并按最大宽度换行。
     */
    private fun drawMinecraftTextBlock(
        g: Graphics2D, text: String, startX: Int, startY: Int, maxWidth: Int
    ) {
        var x = startX
        var y = startY
        var color = style.primaryText
        var i = 0

        while (i < text.length) {
            val c = text[i]

            if (c == '\n') {
                x = startX
                y += DESCRIPTION_LINE_HEIGHT
                i++
                continue
            }

            if (c == '§' && i + 1 < text.length) {
                mcColors[text[i + 1]]?.let { color = it }
                i += 2
                continue
            }

            val charWidth = g.fontMetrics.charWidth(c)
            if (x + charWidth > startX + maxWidth) {
                x = startX
                y += DESCRIPTION_LINE_HEIGHT
            }

            drawStyledText(g, c.toString(), x, y, color)
            x += charWidth
            i++
        }
    }

    /**
     * 绘制带轻微阴影的文本，提升深色背景上的可读性。
     */
    private fun drawStyledText(g: Graphics2D, text: String, x: Int, y: Int, color: Color) {
        g.color = style.shadowColor
        g.drawString(text, x + style.shadowOffset, y + style.shadowOffset)
        g.color = color
        g.drawString(text, x, y)
    }

    /**
     * 估算玩家名称胶囊需要的行数，用于提前计算卡片高度。
     */
    private fun estimatePlayerRows(g: Graphics2D, players: List<Player>, maxWidth: Int): Int {
        if (players.isEmpty()) return 1

        val metrics = g.getFontMetrics(chipFont)
        var rows = 1
        var lineWidth = 0

        players.forEach { player ->
            val chipWidth = metrics.stringWidth(player.name) + PLAYER_CHIP_PADDING_X * 2
            if (lineWidth > 0 && lineWidth + chipWidth > maxWidth) {
                rows++
                lineWidth = chipWidth + PLAYER_GAP_X
            } else {
                lineWidth += chipWidth + PLAYER_GAP_X
            }
        }

        return rows
    }

    /**
     * 在不实际绘制的情况下，估算描述文本需要的行数。
     */
    private fun measureMinecraftTextLines(g: Graphics2D, text: String, maxWidth: Int): Int {
        var lines = 1
        var width = 0
        var i = 0

        while (i < text.length) {
            val c = text[i]

            if (c == '\n') {
                lines++
                width = 0
                i++
                continue
            }

            if (c == '§' && i + 1 < text.length) {
                i += 2
                continue
            }

            val charWidth = g.fontMetrics.charWidth(c)
            if (width + charWidth > maxWidth) {
                lines++
                width = 0
            }

            width += charWidth
            i++
        }

        return lines
    }

    /**
     * 把多张单服务器卡片纵向拼接成一张总图。
     *
     * 该方法适合批量 ping 或已经拿到多张卡片图片流的场景，
     * 会自动解码每一张 PNG，并直接纵向拼接成一张总图。
     */
    fun composeImageList(images: List<ByteArrayOutputStream>): ByteArrayOutputStream {
        if (images.isEmpty()) {
            return generateConnectionFailed(
                serverName = "SERVER OVERVIEW",
                host = "-",
                port = 0,
                errorMessage = "No images to compose"
            )
        }

        val decodedImages: List<BufferedImage> = images.map { output ->
            ImageIO.read(output.toInput()) ?: error("Failed to decode image while composing overview")
        }

        val width = decodedImages.maxOf { image -> image.width }
        val height = decodedImages.sumOf { image -> image.height } + OVERVIEW_GAP * (decodedImages.size - 1) + OVERVIEW_FOOTER_HEIGHT
        val overviewImage = createImage(width, height)
        val generatedAt = LocalDateTime.now().format(overviewTimeFormatter)
        val footerLeftText = "Servers: ${decodedImages.size}   Generated: $generatedAt"
        val footerRightText = "by: limbang"

        overviewImage.withGraphics { g ->
            g.applyImageQuality()

            var currentY = 0
            decodedImages.forEach { cardImage ->
                val cardX = (width - cardImage.width) / 2
                g.drawImage(cardImage, cardX, currentY, null)
                currentY += cardImage.height + OVERVIEW_GAP
            }

            drawOverviewFooter(g, footerLeftText, footerRightText, width, height)
        }

        return overviewImage.toOutputStream("png")
    }

    /**
     * 在总图底部绘制轻量摘要信息。
     *
     * 这里只展示服务器数量、生成时间和作者信息，
     * 便于在群聊中查看图片时快速确认上下文。
     */
    private fun drawOverviewFooter(
        g: Graphics2D,
        footerLeftText: String,
        footerRightText: String,
        width: Int,
        height: Int
    ) {
        val footerTop = height - OVERVIEW_FOOTER_HEIGHT
        g.color = Color(7, 15, 30, 210)
        g.fillRect(0, footerTop, width, OVERVIEW_FOOTER_HEIGHT)
        g.color = style.panelStroke
        g.fillRect(0, footerTop, width, 1)
        g.font = footerFont
        drawStyledText(g, footerLeftText, 18, footerTop + 25, style.secondaryText)
        val footerRightX = width - 18 - g.fontMetrics.stringWidth(footerRightText)
        drawStyledText(g, footerRightText, footerRightX, footerTop + 25, style.secondaryText)
    }

    /**
     * 绘制类似客户端服务器列表中的延迟信号条。
     */
    private fun drawPingBars(g: Graphics2D, ping: Int, x: Int, y: Int) {
        val bars = when {
            ping < 0 -> 0
            ping < 50 -> 5
            ping < 100 -> 4
            ping < 200 -> 3
            ping < 300 -> 2
            else -> 1
        }

        for (i in 0 until 5) {
            val barHeight = 4 + i * 4
            val barX = x + i * 8
            val barY = y - barHeight
            g.color = if (i < bars) pingColor(ping) else style.signalOff
            g.fillRoundRect(barX, barY, 6, barHeight, 4, 4)
        }
    }

    /**
     * 基于服务器关键状态生成稳定随机种子。
     *
     * 这样背景装饰对同一服务器会相对稳定，但不同服务器之间又有差异。
     */
    private fun computeBackgroundSeed(serverName: String, status: ServerStatus, delay: Int): Int {
        var result = serverName.hashCode()
        result = 31 * result + status.versionName.hashCode()
        result = 31 * result + status.playerInfo.playerMax
        result = 31 * result + status.playerInfo.playerOnline
        result = 31 * result + delay
        return result
    }

    /**
     * 基于失败上下文生成背景随机种子。
     */
    private fun computeFailureSeed(serverName: String, host: String, port: Int, errorMessage: String): Int {
        var result = serverName.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + port
        result = 31 * result + errorMessage.hashCode()
        return result
    }

    /**
     * 把延迟数值格式化为展示文本。
     */
    private fun formatPingText(delay: Int): String {
        return if (delay >= 0) "${delay}ms" else "Timeout"
    }

    /**
     * 把连接失败异常文案收敛为更适合展示的单行文本。
     */
    private fun normalizeFailureMessage(errorMessage: String?): String {
        if (errorMessage.isNullOrBlank()) return "Unable to reach the server"
        return errorMessage.lineSequence().first().trim().ifBlank { "Unable to reach the server" }
    }

    /**
     * 根据延迟区间选择延迟文本和信号条颜色。
     */
    private fun pingColor(delay: Int): Color = when {
        delay < 0 -> Color(149, 165, 189)
        delay < 80 -> Color(72, 242, 191)
        delay < 150 -> Color(255, 228, 108)
        delay < 300 -> Color(255, 177, 87)
        else -> Color(255, 106, 124)
    }

    private data class TechStyle(
        val backgroundTop: Color,
        val backgroundBottom: Color,
        val panelOuter: Color,
        val panelInner: Color,
        val panelStroke: Color,
        val panelGlow: Color,
        val badgeFill: Color,
        val badgeBorder: Color,
        val metaFill: Color,
        val metaBorder: Color,
        val iconStroke: Color,
        val chipFill: Color,
        val chipBorder: Color,
        val divider: Color,
        val primaryText: Color,
        val secondaryText: Color,
        val highlightText: Color,
        val shadowColor: Color,
        val shadowOffset: Int,
        val signalOff: Color
    )
}
