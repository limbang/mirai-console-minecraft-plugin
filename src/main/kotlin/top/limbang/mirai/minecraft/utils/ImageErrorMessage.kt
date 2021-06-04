package top.limbang.mirai.minecraft.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO


/**
 * ### 图片错误消息
 *
 * @author limbang
 *
 * 创建时间 2020-09-18
 */
object ImageErrorMessage {

    /**
     * ### 创建图片错误消息
     *
     * - [originalImage] 旧图片
     * - [errorMsg] 错误消息
     */
    suspend fun createImage(originalImage: InputStream, errorMsg: String) = withContext(Dispatchers.Default) {
        val srcImg = ImageIO.read(originalImage)
        val bufImg = BufferedImage(srcImg.width, srcImg.height, BufferedImage.TYPE_INT_RGB)
        val g2d = bufImg.createGraphics()
        g2d.drawImage(srcImg, 0, 0, srcImg.width, srcImg.height, null)

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)

        val font = Font("微软雅黑", Font.PLAIN, 32)
        val vector = font.createGlyphVector(g2d.fontRenderContext, errorMsg)
        val shape = vector.outline!!
        val bounds = shape.bounds!!
        g2d.translate(
            (srcImg.width - bounds.width) / 2 - bounds.x,
            (srcImg.height - bounds.height) - (bounds.y / 2)
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
        return@withContext ByteArrayInputStream(byteArrayOutputStream.toByteArray())
    }

}