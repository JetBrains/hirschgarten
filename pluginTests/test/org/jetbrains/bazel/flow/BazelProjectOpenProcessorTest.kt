package org.jetbrains.bazel.flow

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.testFramework.junit5.SystemProperty
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginConstants
import org.junit.jupiter.api.Test
import java.nio.file.FileAlreadyExistsException
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists

private const val TEMP_DIRECTORY_WORKSPACE_FILE_NAME = "WORKSPACE.bzlmod"

@TestApplication
internal class BazelProjectOpenProcessorTest {

  private val directoryRoot by tempPathFixture()

  @Test
  @SystemProperty(BazelFeatureFlags.AUTO_OPEN_PROJECT_IF_PRESENT, "false")
  fun `should not open directory with dot idea when auto open is disabled`() {
    createDotIdeaDirectory()
    createWorkspaceFile()

    bazelProjectOpenProcessor().canOpenProject(refreshDirectoryRoot()) shouldBe false
  }

  @Test
  @SystemProperty(BazelFeatureFlags.AUTO_OPEN_PROJECT_IF_PRESENT, "true")
  fun `should open directory with dot idea when auto open is enabled`() {
    createDotIdeaDirectory()
    createWorkspaceFile()

    bazelProjectOpenProcessor().canOpenProject(refreshDirectoryRoot()) shouldBe true
  }

  @Test
  @SystemProperty(BazelFeatureFlags.AUTO_OPEN_PROJECT_IF_PRESENT, "true")
  fun `should not open directory without workspace files`() {
    createDotIdeaDirectory()

    bazelProjectOpenProcessor().canOpenProject(refreshDirectoryRoot()) shouldBe false
  }

  @Test
  @SystemProperty(BazelFeatureFlags.AUTO_OPEN_PROJECT_IF_PRESENT, "true")
  fun `should not open directory when only the temp directory holds a workspace file`() {
    withWorkspaceFileInTempDirectory {
      bazelProjectOpenProcessor().canOpenProject(refreshDirectoryRoot()) shouldBe false
    }
  }

  private fun withWorkspaceFileInTempDirectory(action: () -> Unit) {
    val workspaceFile = directoryRoot.parent.resolve(TEMP_DIRECTORY_WORKSPACE_FILE_NAME)
    val created = try {
      workspaceFile.createFile()
      true
    }
    catch (_: FileAlreadyExistsException) {
      false
    }
    try {
      action()
    } finally {
      if (created) workspaceFile.deleteIfExists()
    }
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
}
