import top.limbang.minecraft.entity.ForgeData
import top.limbang.minecraft.entity.Player
import top.limbang.minecraft.entity.PlayerInfo
import top.limbang.minecraft.entity.ServerStatus
import top.limbang.minecraft.utlis.ServerStatusImageGenerator
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ServerStatusImageGeneratorTest {

    private val outputDirectory = File("build/generated-test-images/server-status")

    /**
     * 验证当服务器未返回任何玩家列表时，生成器会折叠底部玩家区域，
     * 从而输出一张比默认高度更紧凑的图片。
     */
    @Test
    fun shouldGenerateCompactImageWhenNoPlayersReturned() {
        val output = ServerStatusImageGenerator.generate(
            serverName = "零人服务器",
            delay = 48,
            status = buildStatus(players = emptyList(), online = 0, max = 20)
        )

        writePreviewImage("no-players.png", output)
        assertPngOutput(output)
        val image = readImage(output)
        assertEquals(760, image.width)
        assertTrue(image.height < 340, "没有玩家列表时，图片高度应小于默认高度 340")
    }

    /**
     * 验证当玩家数量正好为 12 时，生成器可以完整渲染玩家列表，
     * 并保持玩家区域的标准布局高度。
     */
    @Test
    fun shouldRenderAllPlayersWhenPlayerCountIsExactlyTwelve() {
        val players = buildPlayers(12)

        val output = ServerStatusImageGenerator.generate(
            serverName = "十二人服务器",
            delay = 96,
            status = buildStatus(players = players, online = 12, max = 60)
        )

        writePreviewImage("exactly-twelve-players.png", output)
        assertPngOutput(output)
        val image = readImage(output)
        assertEquals(760, image.width)
        assertTrue(image.height >= 340, "存在玩家列表时，图片高度应至少达到默认高度 340")
    }

    /**
     * 验证当返回的玩家数量超过 12 时，生成器只展示前 12 个，
     * 并为“还有 X 个玩家未展示”的提示额外预留底部空间。
     */
    @Test
    fun shouldReserveExtraHeightWhenMoreThanTwelvePlayersReturned() {
        val twelvePlayersOutput = ServerStatusImageGenerator.generate(
            serverName = "十二人服务器",
            delay = 125,
            status = buildStatus(players = buildPlayers(12), online = 12, max = 60)
        )
        val thirteenPlayersOutput = ServerStatusImageGenerator.generate(
            serverName = "十三人服务器",
            delay = 125,
            status = buildStatus(players = buildPlayers(13), online = 13, max = 60)
        )
        writePreviewImage("twelve-players-baseline.png", twelvePlayersOutput)
        writePreviewImage("more-than-twelve-players.png", thirteenPlayersOutput)
        val twelvePlayersImage = readImage(twelvePlayersOutput)
        val thirteenPlayersImage = readImage(thirteenPlayersOutput)

        assertTrue(
            thirteenPlayersImage.height > twelvePlayersImage.height,
            "超过 12 个玩家时，应为“还有 X 个玩家未展示”预留额外高度"
        )
    }

    /**
     * 验证当延迟为超时状态时，生成器仍然可以正常输出合法图片，
     * 并且不会因为负数延迟导致渲染失败。
     */
    @Test
    fun shouldGenerateValidImageWhenPingTimeoutOccurs() {
        val output = ServerStatusImageGenerator.generate(
            serverName = "超时服务器",
            delay = -1,
            status = buildStatus(players = buildPlayers(3), online = 3, max = 20)
        )

        writePreviewImage("ping-timeout.png", output)
        val image = readImage(output)
        assertEquals(760, image.width)
        assertTrue(image.height >= 340)
    }

    /**
     * 验证 NeoForge 在 `fmlNetworkVersion = 4` 的 ping-only 场景下，
     * 即使拿不到模组列表，也仍然可以正常生成合法图片。
     */
    @Test
    fun shouldGenerateValidImageForNeoForgePingOnlyStatus() {
        val output = ServerStatusImageGenerator.generate(
            serverName = "NeoForge 服务器",
            delay = 72,
            status = buildStatus(
                players = buildPlayers(5),
                online = 5,
                max = 50,
                forgeData = ForgeData(
                    fmlNetworkVersion = 4,
                    truncated = false,
                    mods = emptyList(),
                    channels = emptyList()
                )
            )
        )

        writePreviewImage("neoforge-ping-only.png", output)
        assertPngOutput(output)
    }

    /**
     * 验证连接失败专用图片可以稳定生成，
     * 并且在失败场景下仍然返回合法 PNG，而不是抛出异常或返回空内容。
     */
    @Test
    fun shouldGenerateFailureImageWhenConnectionFails() {
        val output = ServerStatusImageGenerator.generateConnectionFailed(
            serverName = "连接失败服务器",
            host = "127.0.0.1",
            port = 25565,
            errorMessage = "Connection refused"
        )

        writePreviewImage("connection-failed.png", output)
        assertPngOutput(output)
        val image = readImage(output)
        assertEquals(760, image.width)
        assertTrue(image.height > 200, "失败卡片应保持足够的可读高度")
    }

    /**
     * 验证多张服务器卡片可以纵向合成为一张总图，
     * 并且总图尺寸会明显大于单张卡片，方便查看批量结果。
     */
    @Test
    fun shouldComposeMultipleServerImagesIntoOneOverview() {
        val firstOutput = ServerStatusImageGenerator.generate(
            serverName = "服务器一",
            delay = 36,
            status = buildStatus(players = buildPlayers(2), online = 2, max = 20)
        )
        val secondOutput = ServerStatusImageGenerator.generate(
            serverName = "服务器二",
            delay = 128,
            status = buildStatus(players = buildPlayers(12), online = 12, max = 40)
        )
        val thirdOutput = ServerStatusImageGenerator.generateConnectionFailed(
            serverName = "服务器三",
            host = "192.168.1.1",
            port = 25565,
            errorMessage = "Connection timed out"
        )

        val overviewOutput = ServerStatusImageGenerator.composeImageList(
            listOf(firstOutput, secondOutput, thirdOutput)
        )

        writePreviewImage("server-overview.png", overviewOutput)
        assertPngOutput(overviewOutput)
        val overviewImage = readImage(overviewOutput)
        assertEquals(760, overviewImage.width, "总图宽度应与单张卡片宽度保持一致")
        assertTrue(overviewImage.height > 900, "总图高度应大于任意单张卡片高度")
    }

    /**
     * 构造一份用于图片生成测试的服务器状态对象。
     *
     * 该方法用于集中管理测试默认值，避免每个测试重复拼装状态数据，
     * 同时方便针对单个字段做最小改动来覆盖不同渲染分支。
     */
    private fun buildStatus(
        players: List<Player>,
        online: Int,
        max: Int,
        description: String = "§b科技风状态图 §f测试文本\n第二行描述内容",
        versionName: String = "1.20.1",
        forgeData: ForgeData = ForgeData(
            fmlNetworkVersion = 0,
            truncated = false,
            mods = emptyList(),
            channels = emptyList()
        )
    ): ServerStatus {
        return ServerStatus(
            favicon = "",
            description = description,
            playerInfo = PlayerInfo(
                playerMax = max,
                playerOnline = online,
                players = players
            ),
            versionName = versionName,
            versionNumber = 763,
            forgeData = forgeData
        )
    }

    /**
     * 按指定数量生成测试玩家列表。
     *
     * 玩家名称和 UUID 都是稳定可预测的占位数据，
     * 便于构造 0、12、13 等不同数量场景。
     */
    private fun buildPlayers(count: Int): List<Player> {
        return (1..count).map { index ->
            Player(
                name = "Player$index",
                id = "00000000-0000-0000-0000-${index.toString().padStart(12, '0')}"
            )
        }
    }

    /**
     * 断言输出流非空且内容符合 PNG 文件头格式。
     *
     * 该断言用于快速验证生成器返回的内容类型正确，
     * 避免出现空流或非图片数据仍被误当作成功输出的情况。
     */
    private fun assertPngOutput(output: ByteArrayOutputStream) {
        assertTrue(output.size() > 0, "输出流不应为空")
        val pngHeader = output.toByteArray().take(8).map { it.toInt() and 0xFF }
        assertEquals(listOf(137, 80, 78, 71, 13, 10, 26, 10), pngHeader, "输出内容应为 PNG")
    }

    /**
     * 把输出流解码为图片对象，便于继续校验尺寸等渲染结果。
     */
    private fun readImage(output: ByteArrayOutputStream): BufferedImage {
        val image = ImageIO.read(ByteArrayInputStream(output.toByteArray()))
        assertNotNull(image, "输出流应能被解码为图片")
        return image
    }

    /**
     * 把测试生成的 PNG 写入 `build/generated-test-images/server-status/` 目录，
     * 方便在测试执行后直接查看不同场景的实际渲染效果。
     */
    private fun writePreviewImage(fileName: String, output: ByteArrayOutputStream) {
        outputDirectory.mkdirs()
        File(outputDirectory, fileName).writeBytes(output.toByteArray())
    }
}
