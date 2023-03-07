package org.jetbrains.plugins.bsp.flow.open.wizard

import ch.epfl.scala.bsp4j.BspConnectionDetails
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

class ConnectionFileTest: MockProjectBaseTest() {

  private lateinit var filePath: Path

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    this.filePath = kotlin.io.path.createTempFile("connection-file", ".json")
  }

  @AfterEach
  fun afterEach() {
    filePath.deleteIfExists()
  }

  @Test
  fun `should correctly compare 2 versions of ConnectionFile`() {
    // given
    val v1 = generateConnectionFile("1.2.3")
    val v2 = generateConnectionFile("1.2.4")
    val v3 = generateConnectionFile("2.2.1")
    val v4 = generateConnectionFile("3")
    val v5 = generateConnectionFile("1.2.3")
    val v6 = generateConnectionFile("1.12.3")
    val v7 = generateConnectionFile("1.12.3-EAP")

    // then
    v1 shouldBeLessThan v2
    v2 shouldBeLessThan v3
    v3 shouldBeGreaterThan v2
    v4 shouldBeGreaterThan v3
    v5 shouldBeEqualComparingTo v1
    v6 shouldBeGreaterThan v5
    v7 shouldBeLessThan v6
  }

  private fun generateConnectionFile(version: String) =
    ConnectionFile(
      LocatedBspConnectionDetails(
        bspConnectionDetails = BspConnectionDetails(
          "",
          listOf(),
          version,
          "",
          listOf(),
        ),
        connectionFileLocation = filePath.toVirtualFile(),
      )
    )
}