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
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import javax.imageio.ImageIO


/**
 * 解码 Minecraft 服务器图标
 *
 * @param favicon
 */
fun decodeFavicon(favicon: String): ByteArray {
    // 如果图标为空读取默认图标
    if (favicon.isEmpty()) {
        val imageStream = object {}::class.java.classLoader.getResourceAsStream("images/defaultFavicon.png")!!
        return imageStream.use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
                outputStream.toByteArray()
            }
        }
    }
    val prefix = "data:image/png;base64,"
    if (!favicon.startsWith(prefix)) throw RuntimeException("Unknown format")
    val faviconBase64 = favicon.substring(prefix.length).replace("\n", "")
    val byte = try {
        Base64.getDecoder().decode(faviconBase64.toByteArray(StandardCharsets.UTF_8))
    } catch (e: Exception) {
        throw RuntimeException("Malformed base64 server icon")
    }
    return byte
}

/**
 * 编码 Minecraft 服务器图标
 *
 * @param byte
 */
fun encodeFavicon(byte: ByteArray): String {
    val prefix = "data:image/png;base64,"
    val faviconBase64 = Base64.getEncoder().encodeToString(byte)
    return prefix + faviconBase64
}

/**
 * ### 添加字幕
 * @param subtitles 字幕
 */
fun BufferedImage.addSubtitles(subtitles: String) : ByteArrayOutputStream {
    val bufImg = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g2d = bufImg.createGraphics()
    g2d.drawImage(this, 0, 0, width, height, null)

    // 抗锯齿相关
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)

    val font = Font("微软雅黑", Font.PLAIN, 32)
    val vector = font.createGlyphVector(g2d.fontRenderContext, subtitles)
    val shape = vector.outline!!
    val bounds = shape.bounds!!
    g2d.translate(
        (width - bounds.width) / 2 - bounds.x, (height - bounds.height) - (bounds.y / 2)
    )
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.stroke = BasicStroke(3.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g2d.color = Color(63, 63, 63, 150)
    g2d.draw(shape)
    g2d.color = Color.WHITE
    g2d.fill(shape)
    g2d.dispose()

    val byteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.write(bufImg, "jpg", byteArrayOutputStream)
    return byteArrayOutputStream
}

/**
 * ### 从输入流读取图片
 */
fun InputStream.readImage(): BufferedImage {
    return ImageIO.read(this)
}

/**
 * 文本转成白底黑字的图片
 *
 * @return
 */
fun String.toImage() = textToImage(this)

/**
 * 文本转成白底黑字的图片
 *
 * @param text
 * @return
 */
private fun textToImage(text: String): ByteArrayOutputStream {
    //按照换行符分割文本，并设置字体、行距等参数
    val textList = text.split("\n")
    val g2d = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics()
    g2d.font = Font("微软雅黑", Font.PLAIN, 20)
    val fm = g2d.fontMetrics
    val lineHeight = fm.height

    //计算每行文本的宽度和高度，并找出最长一行的宽度
    var maxWidth = 0
    for (line in textList) {
        val lineWidth = fm.stringWidth(line)
        if (lineWidth > maxWidth) {
            maxWidth = lineWidth
        }
    }
    val imageWidth = maxWidth + 20 //左右各留 10 像素的边距
    val imageHeight = lineHeight * textList.size + 20 //上下各留 10 像素的边距

    //创建一个新的 BufferedImage 对象并获取 Graphics2D 对象
    val newImage = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB)
    val newG2d = newImage.createGraphics()

    //循环绘制每行文本到新的 BufferedImage 上，并释放资源
    newG2d.color = Color.WHITE
    newG2d.fillRect(0, 0, imageWidth, imageHeight) //先用白色填充整个区域
    newG2d.font = g2d.font //复制之前的字体
    newG2d.color = Color.BLACK //设置字体颜色为黑色
    for ((index, line) in textList.withIndex()) {
        newG2d.drawString(line, 10, lineHeight * (index + 1)) //在 (10, lineHeight * (index + 1)) 的位置开始绘制文本
    }
    newG2d.dispose()
    g2d.dispose()

    val byteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.write(newImage, "png", byteArrayOutputStream)
    return byteArrayOutputStream
}

/**
 * ### ByteArrayOutputStream 转 ByteArrayInputStream
 *
 */
fun ByteArrayOutputStream.toInput(): ByteArrayInputStream {
    return ByteArrayInputStream(this.toByteArray())
}
