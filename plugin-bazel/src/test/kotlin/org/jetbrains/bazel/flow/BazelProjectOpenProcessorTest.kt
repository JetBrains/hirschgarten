package org.jetbrains.bazel.flow

import io.kotest.matchers.collections.shouldContain
import org.jetbrains.bazel.flow.open.BUILD_FILE_GLOB
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.listDirectoryEntries

private const val BUILD_FILE_NAME = "BUILD.bazel"

class BazelProjectOpenProcessorTest {
  private lateinit var directoryRoot: Path

  @BeforeEach
  fun setUp() {
    directoryRoot = createTempDirectory("dir").also { it.toFile().deleteOnExit() }
  }

  @AfterEach
  fun tearDown() {
    directoryRoot.toFile().deleteRecursively()
  }

  @Test
  fun `should find out build file with BUILD_FILE_GLOB`() {
    val buildFile = directoryRoot.resolve(BUILD_FILE_NAME)
    buildFile.also { it.createFile() }
    directoryRoot.listDirectoryEntries(glob = BUILD_FILE_GLOB) shouldContain buildFile
  }

  private fun Path.createFile() = this.toFile().createNewFile()
}
