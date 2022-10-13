package org.jetbrains.plugins.bsp.protocol.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

class LocatedBspConnectionDetailsParserTest : MockProjectBaseTest() {

  private lateinit var filePath: Path

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    this.filePath = createTempFile("connection-file", ".json")
  }

  @AfterEach
  fun afterEach() {
    filePath.deleteIfExists()
  }

  @Test
  fun `should return null if file contains invalid json`() {
    // given
    filePath.writeText(
      """
        |{
        | "name": "build tool",
        | "argv": ["build-tool", "bsp"],
        | "version": "1.2.3
        | "languages": ["scala", "kotlin"]
        |}
      """.trimMargin()
    )

    // when
    val locatedBspConnectionDetails = LocatedBspConnectionDetailsParser.parseFromFile(filePath.toVirtualFile())

    // then
    locatedBspConnectionDetails shouldBe null
  }

  @Test
  fun `should parse if file contains valid json`() {
    // given
    filePath.writeText(
      """
        |{
        | "name": "build tool",
        | "argv": ["build-tool", "bsp"],
        | "version": "1.2.37",
        | "bspVersion": "2.0.0",
        | "languages": ["scala", "kotlin"]
        |}
      """.trimMargin()
    )

    // when
    val locatedBspConnectionDetails = LocatedBspConnectionDetailsParser.parseFromFile(filePath.toVirtualFile())

    // then
    val expectedLocatedBspConnectionDetails =
      LocatedBspConnectionDetails(
        bspConnectionDetails = BspConnectionDetails(
          "build tool",
          listOf("build-tool", "bsp"),
          "1.2.37",
          "2.0.0",
          listOf("scala", "kotlin"),
        ),
        connectionFileLocation = filePath.toVirtualFile(),
      )
    locatedBspConnectionDetails shouldBe expectedLocatedBspConnectionDetails
  }
}
