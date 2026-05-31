/*
 * Copyright (c) 2023 limbang and contributors.
 *
 * 此源代码的使用受 GNU AGPLv3 许可证的约束，该许可证可在"LICENSE"文件中找到。
 * Use of this source code is governed by the GNU AGPLv3 license that can be found in the "LICENSE" file.
 */

package top.limbang.minecraft.utlis

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import javax.imageio.ImageIO

private const val DEFAULT_FAVICON_PATH = "images/defaultFavicon.png"
private const val FAVICON_PREFIX = "data:image/png;base64,"

/**
 * 把 Minecraft 服务器状态中的 favicon 字符串解码为原始图片字节数组。
 *
 * 支持两种输入：
 * - 标准的 `data:image/png;base64,...` 形式
 * - 空字符串或空白字符串，此时自动回退到项目内置默认图标
 *
 * @param favicon 服务器状态响应中的 favicon 字符串
 * @return 解码后的 PNG 字节数组
 * @throws RuntimeException 当 favicon 格式不符合预期或 Base64 内容损坏时抛出
 */
fun decodeFavicon(favicon: String): ByteArray {
    if (favicon.isBlank()) return readResourceBytes(DEFAULT_FAVICON_PATH)

    if (!favicon.startsWith(FAVICON_PREFIX)) throw RuntimeException("Unknown format")

    val faviconBase64 = favicon.substring(FAVICON_PREFIX.length).replace("\n", "")
    return try {
        Base64.getDecoder().decode(faviconBase64.toByteArray(StandardCharsets.UTF_8))
    } catch (_: Exception) {
        throw RuntimeException("Malformed base64 server icon")
    }
}

/**
 * 把原始 PNG 字节编码为 Minecraft 服务器状态需要的 favicon 字符串格式。
 *
 * @param byte PNG 图片字节
 * @return `data:image/png;base64,...` 形式的字符串
 */
fun encodeFavicon(byte: ByteArray): String {
    val faviconBase64 = Base64.getEncoder().encodeToString(byte)
    return FAVICON_PREFIX + faviconBase64
}

/**
 * 从项目资源目录中读取默认 favicon 图片。
 *
 * @return 解码后的默认图标图片
 * @throws IllegalStateException 当默认图标资源缺失时抛出
 */
fun loadDefaultFaviconImage(): BufferedImage {
    return readResourceImage(DEFAULT_FAVICON_PATH)
}

/**
 * 直接把 favicon 字符串解码为 [BufferedImage]。
 *
 * 这是 [decodeFavicon] 的图片层封装，适合直接进入绘图流程的调用场景。
 *
 * @param favicon 服务器状态响应中的 favicon 字符串
 * @return 解码后的图片对象
 * @throws RuntimeException 当 favicon 无法被解析成图片时抛出
 */
fun decodeFaviconImage(favicon: String): BufferedImage {
    return ByteArrayInputStream(decodeFavicon(favicon)).use(ImageIO::read)
        ?: throw RuntimeException("Failed to decode favicon image")
}

/**
 * 创建一个新的内存图片缓冲区。
 *
 * @param width 图片宽度，单位像素
 * @param height 图片高度，单位像素
 * @param type Java2D 图片类型，默认为 `TYPE_INT_ARGB`
 * @return 新建的 [BufferedImage]
 */
fun createImage(width: Int, height: Int, type: Int = BufferedImage.TYPE_INT_ARGB): BufferedImage {
    return BufferedImage(width, height, type)
}

/**
 * 创建一个专门用于文本测量的临时绘图上下文。
 *
 * 返回的 [Graphics2D] 已经应用高质量渲染参数，并设置好了传入字体。
 * 调用方在使用完成后需要自行 `dispose()`。
 *
 * @param font 用于测量的字体
 * @return 已配置好的测量用绘图上下文
 */
fun createMeasureGraphics(font: Font): Graphics2D {
    return createImage(1, 1).createGraphics().applyImageQuality(font)
}

/**
 * 按指定格式把图片写入已有输出流。
 *
 * @param outputStream 输出目标
 * @param format 图片格式，例如 `png`、`jpg`
 */
fun BufferedImage.writeTo(outputStream: ByteArrayOutputStream, format: String) {
    ImageIO.write(this, format, outputStream)
}

/**
 * 按指定格式把图片编码到一个新的 [ByteArrayOutputStream] 中。
 *
 * @param format 图片格式，例如 `png`、`jpg`
 * @return 编码后的输出流
 */
fun BufferedImage.toOutputStream(format: String): ByteArrayOutputStream {
    return ByteArrayOutputStream().also { writeTo(it, format) }
}

/**
 * 为 [Graphics2D] 应用统一的高质量渲染参数。
 *
 * 该方法用于统一图片绘制质量，保证文本、描边和填充的观感一致。
 *
 * @param font 可选字体，传入后会先设置到当前绘图上下文
 * @return 当前 [Graphics2D] 本身，便于链式调用
 */
fun Graphics2D.applyImageQuality(font: Font? = null): Graphics2D {
    if (font != null) this.font = font
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
    setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    return this
}

/**
 * 在当前绘图上下文中绘制一个心形路径。
 *
 * 该工具只负责生成和输出心形轮廓，具体是描边还是填充由调用方决定：
 * - 调用 `drawHeartShape(...)` 后再执行 `draw(...)`，可绘制心形描边
 * - 调用 `drawHeartShape(...)` 后再执行 `fill(...)`，可绘制实心心形
 *
 * 这里使用相对平滑的贝塞尔曲线来构造心形，适合做轻量背景装饰、
 * 徽标点缀或氛围图形。
 *
 * @param centerX 心形中心点 X 坐标
 * @param centerY 心形中心点 Y 坐标
 * @param width 心形总宽度
 * @param height 心形总高度
 * @return 可直接用于 `draw` / `fill` 的心形路径
 */
fun drawHeartShape(centerX: Float, centerY: Float, width: Float, height: Float): Path2D.Float {
    val halfWidth = width / 2f
    val halfHeight = height / 2f
    val left = centerX - halfWidth
    val top = centerY - halfHeight
    val right = centerX + halfWidth
    val bottom = centerY + halfHeight
    val upperY = top + height * 0.32f
    val controlOffsetX = width * 0.28f
    val controlOffsetY = height * 0.22f

    return Path2D.Float().apply {
        moveTo(centerX, bottom)
        curveTo(
            left - controlOffsetX * 0.1f, centerY + controlOffsetY,
            left, upperY + controlOffsetY,
            left + width * 0.18f, upperY
        )
        curveTo(
            left + width * 0.28f, top,
            centerX - width * 0.1f, top,
            centerX, upperY - controlOffsetY * 0.55f
        )
        curveTo(
            centerX + width * 0.1f, top,
            right - width * 0.28f, top,
            right - width * 0.18f, upperY
        )
        curveTo(
            right, upperY + controlOffsetY,
            right + controlOffsetX * 0.1f, centerY + controlOffsetY,
            centerX, bottom
        )
        closePath()
    }
}

/**
 * 在临时绘图上下文中执行绘制逻辑，并在结束后自动释放资源。
 *
 * @param block 接收绘图上下文的绘制逻辑
 * @return [block] 的返回值
 */
inline fun <T> BufferedImage.withGraphics(block: (Graphics2D) -> T): T {
    val graphics = createGraphics().applyImageQuality()
    return try {
        block(graphics)
    } finally {
        graphics.dispose()
    }
}

/**
 * 在当前图片副本上居中绘制带描边的字幕。
 *
 * 这里输出 JPEG 是因为该工具主要用于简单字幕叠加，而不是保留透明通道的素材加工。
 *
 * @param subtitles 要绘制在图片底部附近的字幕文本
 * @return 合成后图片的 JPEG 输出流
 */
fun BufferedImage.addSubtitles(subtitles: String): ByteArrayOutputStream {
    val composed = createImage(width, height, BufferedImage.TYPE_INT_RGB)
    composed.withGraphics { g2d ->
        g2d.drawImage(this, 0, 0, width, height, null)

        val font = Font("微软雅黑", Font.PLAIN, 32)
        val vector = font.createGlyphVector(g2d.fontRenderContext, subtitles)
        val shape = vector.outline
        val bounds = shape.bounds

        g2d.translate(
            (width - bounds.width) / 2 - bounds.x, (height - bounds.height) - (bounds.y / 2)
        )
        g2d.stroke = BasicStroke(3.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2d.color = Color(63, 63, 63, 150)
        g2d.draw(shape)
        g2d.color = Color.WHITE
        g2d.fill(shape)
    }

    return composed.toOutputStream("jpg")
}

/**
 * 使用 ImageIO 从输入流中读取图片。
 *
 * @return 解码后的图片对象
 */
fun InputStream.readImage(): BufferedImage {
    return ImageIO.read(this)
}

/**
 * 把普通文本转换为白底黑字的 PNG 图片。
 *
 * @return 渲染后的 PNG 输出流
 */
fun String.toImage() = textToImage(this)

/**
 * 文本转图片的内部实现。
 *
 * 按换行拆分文本后，使用固定中文 UI 字体把每一行绘制到白底图片上。
 *
 * @param text 输入文本，按换行符分割
 * @return 渲染后的 PNG 输出流
 */
private fun textToImage(text: String): ByteArrayOutputStream {
    val textList = text.split("\n")
    val font = Font("微软雅黑", Font.PLAIN, 20)
    val measureGraphics = createMeasureGraphics(font)
    val metrics = measureGraphics.fontMetrics
    val lineHeight = metrics.height
    val maxWidth = textList.maxOfOrNull(metrics::stringWidth) ?: 0
    measureGraphics.dispose()

    val imageWidth = maxWidth + 20
    val imageHeight = lineHeight * textList.size + 20
    val image = createImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB)

    image.withGraphics { graphics ->
        graphics.font = font
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, imageWidth, imageHeight)
        graphics.color = Color.BLACK

        textList.forEachIndexed { index, line ->
            graphics.drawString(line, 10, lineHeight * (index + 1))
        }
    }

    return image.toOutputStream("png")
}

/**
 * 基于当前输出流内容创建一个输入流视图。
 *
 * @return 包含当前字节内容的 [ByteArrayInputStream]
 */
fun ByteArrayOutputStream.toInput(): ByteArrayInputStream {
    return ByteArrayInputStream(this.toByteArray())
}

/**
 * 从应用类路径中读取并解码图片资源。
 *
 * @param path 相对于 `src/main/resources` 的资源路径
 * @return 解码后的图片对象
 * @throws IllegalStateException 当资源不存在时抛出
 */
private fun readResourceImage(path: String): BufferedImage {
    return object {}::class.java.classLoader.getResourceAsStream(path)?.use(ImageIO::read)
        ?: throw IllegalStateException("Missing resource: $path")
}

/**
 * 把类路径中的资源读取为原始字节数组。
 *
 * @param path 相对于 `src/main/resources` 的资源路径
 * @return 资源字节数组
 * @throws IllegalStateException 当资源不存在时抛出
 */
private fun readResourceBytes(path: String): ByteArray {
    return object {}::class.java.classLoader.getResourceAsStream(path)?.use { inputStream ->
        ByteArrayOutputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
            outputStream.toByteArray()
        }
    } ?: throw IllegalStateException("Missing resource: $path")
}
