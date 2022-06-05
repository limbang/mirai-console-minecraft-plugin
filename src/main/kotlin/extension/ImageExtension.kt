/*
 * Copyright 2020-2022 limbang and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/limbang/mirai-console-minecraft-plugin/blob/master/LICENSE
 */

package top.limbang.minecraft.extension


import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

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
