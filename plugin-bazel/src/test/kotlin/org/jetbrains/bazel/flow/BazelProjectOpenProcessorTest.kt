package org.jetbrains.bazel.flow

import io.kotest.matchers.collections.shouldContain
import org.jetbrains.bazel.flow.open.ALL_ELIGIBLE_FILES_GLOB
import org.jetbrains.bazel.flow.open.BUILD_FILE_GLOB
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.listDirectoryEntries

private const val BUILD_FILE_NAME = "BUILD.bazel"
private const val WORKSPACE_FILE_NAME = "WORKSPACE"
private const val MODULE_FILE_NAME = "MODULE.bazel"
private const val PROJECT_VIEW_FILE = "bazel.bazelproject"

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
  fun `should find out build file with ALL_ELIGIBLE_FILES_GLOB`() {
    val buildFile = directoryRoot.resolve(BUILD_FILE_NAME)
    buildFile.also { it.createFile() }
    directoryRoot.listDirectoryEntries(glob = ALL_ELIGIBLE_FILES_GLOB) shouldContain buildFile
  }

  @Test
  fun `should find out workspace file with ALL_ELIGIBLE_FILES_GLOB`() {
    val workspaceFile = directoryRoot.resolve(WORKSPACE_FILE_NAME)
    workspaceFile.also { it.createFile() }
    directoryRoot.listDirectoryEntries(glob = ALL_ELIGIBLE_FILES_GLOB) shouldContain workspaceFile
  }

  @Test
  fun `should find out module file with ALL_ELIGIBLE_FILES_GLOB`() {
    val moduleFile = directoryRoot.resolve(MODULE_FILE_NAME)
    moduleFile.also { it.createFile() }
    directoryRoot.listDirectoryEntries(glob = ALL_ELIGIBLE_FILES_GLOB) shouldContain moduleFile
  }

  @Test
  fun `should find out project view file with ALL_ELIGIBLE_FILES_GLOB`() {
    val projectViewFile = directoryRoot.resolve(PROJECT_VIEW_FILE)
    projectViewFile.also { it.createFile() }
    directoryRoot.listDirectoryEntries(glob = ALL_ELIGIBLE_FILES_GLOB) shouldContain projectViewFile
  }

  @Test
  fun `should find out build file with BUILD_FILE_GLOB`() {
    val buildFile = directoryRoot.resolve(BUILD_FILE_NAME)
    buildFile.also { it.createFile() }
    directoryRoot.listDirectoryEntries(glob = BUILD_FILE_GLOB) shouldContain buildFile
  }

  private fun Path.createFile() = this.toFile().createNewFile()
}
