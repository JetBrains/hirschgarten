package org.jetbrains.bazel.flow

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.flow.open.BUILD_FILE_GLOB
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.listDirectoryEntries

private const val BUILD_FILE_NAME = "BUILD.bazel"

class BazelProjectOpenProcessorTest : BasePlatformTestCase() {
  private lateinit var directoryRoot: Path
  private var originalAutoOpenProjectIfPresent: String? = null

  override fun setUp() {
    super.setUp()
    directoryRoot = createTempDirectory(Path.of("/tmp"), "dir").also { it.toFile().deleteOnExit() }
    originalAutoOpenProjectIfPresent = System.getProperty(BazelFeatureFlags.AUTO_OPEN_PROJECT_IF_PRESENT)
  }

  override fun tearDown() {
    try {
      originalAutoOpenProjectIfPresent
        ?.let { System.setProperty(BazelFeatureFlags.AUTO_OPEN_PROJECT_IF_PRESENT, it) }
        ?: System.clearProperty(BazelFeatureFlags.AUTO_OPEN_PROJECT_IF_PRESENT)
      directoryRoot.toFile().deleteRecursively()
    }
    finally {
      super.tearDown()
    }
  }

  fun `test should find out build file with BUILD_FILE_GLOB`() {
    val buildFile = directoryRoot.resolve(BUILD_FILE_NAME)
    buildFile.also { it.createFile() }
    directoryRoot.listDirectoryEntries(glob = BUILD_FILE_GLOB) shouldContain buildFile
  }

  fun `test should not open directory with dot idea when auto open is disabled`() {
    setAutoOpenProjectIfPresent(false)
    createDotIdeaDirectory()
    createWorkspaceFile()

    bazelProjectOpenProcessor().canOpenProject(refreshDirectoryRoot()) shouldBe false
  }

  fun `test should open directory with dot idea when auto open is enabled`() {
    setAutoOpenProjectIfPresent(true)
    createDotIdeaDirectory()
    createWorkspaceFile()

    bazelProjectOpenProcessor().canOpenProject(refreshDirectoryRoot()) shouldBe true
  }

  fun `test should not open directory without workspace files`() {
    setAutoOpenProjectIfPresent(true)
    createDotIdeaDirectory()

    bazelProjectOpenProcessor().canOpenProject(refreshDirectoryRoot()) shouldBe false
  }

  private fun setAutoOpenProjectIfPresent(value: Boolean) {
    System.setProperty(BazelFeatureFlags.AUTO_OPEN_PROJECT_IF_PRESENT, value.toString())
  }

  private fun createDotIdeaDirectory() {
    directoryRoot.resolve(Project.DIRECTORY_STORE_FOLDER).createDirectories()
  }

  private fun createWorkspaceFile() {
    directoryRoot.resolve(Constants.MODULE_BAZEL_FILE_NAME).createFile()
  }

  private fun refreshDirectoryRoot(): VirtualFile =
    LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directoryRoot)
    ?: error("Cannot refresh test directory $directoryRoot")

  private fun bazelProjectOpenProcessor(): ProjectOpenProcessor =
    ProjectOpenProcessor.EXTENSION_POINT_NAME.extensionList
      .single { it.name == BazelPluginConstants.BAZEL_DISPLAY_NAME }

  private fun Path.createFile() = this.toFile().createNewFile()
}
