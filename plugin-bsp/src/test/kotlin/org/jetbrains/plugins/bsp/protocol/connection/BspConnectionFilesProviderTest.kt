package org.jetbrains.plugins.bsp.protocol.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files.createDirectory
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

class BspConnectionFilesProviderTest : MockProjectBaseTest() {

  private lateinit var projectPath: Path
  private lateinit var provider: BspConnectionFilesProvider

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    this.projectPath = createTempDirectory("project")
    this.provider = BspConnectionFilesProvider(projectPath.toVirtualFile())
  }

  @AfterEach
  fun afterEach() {
    projectPath.toFile().deleteRecursively()
  }

  @Test
  fun `should return false for isAnyBspConnectionFileDefined and en empty list for connectionFiles if there is no configuration files`() {
    // given

    // when & then
    provider.isAnyBspConnectionFileDefined() shouldBe false
    provider.connectionFiles shouldBe emptyList()
  }

  @Test
  fun `should return true for isAnyBspConnectionFileDefined and list with 1 element for connectionFiles if there is 1 configuration file`() {
    // given
    val dotBspDir = createDirectory(projectPath.resolve(".bsp"))

    val connectionFile1 = dotBspDir.resolve("connection-file-1.json").createFile()
    connectionFile1.writeText(
      """
        |{
        | "name": "build tool 1",
        | "argv": ["build-tool-1", "bsp"],
        | "version": "1.2.37",
        | "bspVersion": "2.0.0",
        | "languages": ["scala", "kotlin"]
        |}
      """.trimMargin()
    )

    // when & then
    provider.isAnyBspConnectionFileDefined() shouldBe true

    val expectedConnectionFiles = listOf(
      LocatedBspConnectionDetails(
        bspConnectionDetails = BspConnectionDetails(
          "build tool 1",
          listOf("build-tool-1", "bsp"),
          "1.2.37",
          "2.0.0",
          listOf("scala", "kotlin"),
        ),
        connectionFileLocation = connectionFile1.toVirtualFile(),
      )
    )
    provider.connectionFiles shouldContainExactlyInAnyOrder expectedConnectionFiles
  }

  @Test
  fun `should return true for isAnyBspConnectionFileDefined and list with 3 element for connectionFiles if there are three configuration files and 1 non configuration file and 1 invalid connection file`() {
    // given
    val dotBspDir = createDirectory(projectPath.resolve(".bsp"))

    val connectionFile1 = dotBspDir.resolve("connection-file-1.json").createFile()
    connectionFile1.writeText(
      """
        |{
        | "name": "build tool 1",
        | "argv": ["build-tool-1", "bsp"],
        | "version": "1.2.37",
        | "bspVersion": "2.0.0",
        | "languages": ["scala", "kotlin"]
        |}
      """.trimMargin()
    )

    val connectionFile2 = dotBspDir.resolve("connection-file-2.json").createFile()
    connectionFile2.writeText(
      """
        |{
        | "name": "build tool 2",
        | "argv": ["build-tool-2", "bsp"],
        | "version": "1.2.37",
        | "bspVersion": "2.0.0",
        | "languages": ["scala", "kotlin"]
        |}
      """.trimMargin()
    )

    val nonConnectionFile = dotBspDir.resolve("non-connection-file.xd").createFile()
    nonConnectionFile.writeText("random content")

    val connectionFile3 = dotBspDir.resolve("connection-file-3.json").createFile()
    connectionFile3.writeText(
      """
        |{
        | "name": "build tool 3",
        | "argv": ["build-tool-3", "bsp"],
        | "version": "1.2.37",
        | "bspVersion": "2.0.0",
        | "languages": ["scala", "kotlin"]
        |}
      """.trimMargin()
    )

    val invalidConnectionFile = dotBspDir.resolve("invalid-connection-file.json").createFile()
    invalidConnectionFile.writeText(
      """
        |{
        | "name": "invalid build tool",
        | "argv": ["invalid 
        | "version": "1.2.37",
        | "bspVersion": "2.0.0",
        | "languages": ["scala", "kotlin"]
        |}
      """.trimMargin()
    )

    // when & then
    provider.isAnyBspConnectionFileDefined() shouldBe true

    val expectedConnectionFiles = listOf(
      LocatedBspConnectionDetails(
        bspConnectionDetails = BspConnectionDetails(
          "build tool 1",
          listOf("build-tool-1", "bsp"),
          "1.2.37",
          "2.0.0",
          listOf("scala", "kotlin"),
        ),
        connectionFileLocation = connectionFile1.toVirtualFile(),
      ),
      LocatedBspConnectionDetails(
        bspConnectionDetails = BspConnectionDetails(
          "build tool 2",
          listOf("build-tool-2", "bsp"),
          "1.2.37",
          "2.0.0",
          listOf("scala", "kotlin"),
        ),
        connectionFileLocation = connectionFile2.toVirtualFile(),
      ),
      LocatedBspConnectionDetails(
        bspConnectionDetails = BspConnectionDetails(
          "build tool 3",
          listOf("build-tool-3", "bsp"),
          "1.2.37",
          "2.0.0",
          listOf("scala", "kotlin"),
        ),
        connectionFileLocation = connectionFile3.toVirtualFile(),
      )
    )
    provider.connectionFiles shouldContainExactlyInAnyOrder expectedConnectionFiles
  }
}
