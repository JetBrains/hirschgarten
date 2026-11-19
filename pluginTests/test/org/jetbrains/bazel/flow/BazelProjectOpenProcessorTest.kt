package org.jetbrains.bazel.flow

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.impl.ProjectImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.project.stateStore
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.common.waitUntil
import com.intellij.testFramework.junit5.SystemProperty
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.useProjectAsync
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.jetbrains.bazel.startup.setBazelStartupSyncEnabledInTests
import org.junit.jupiter.api.Test
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

private const val TEMP_DIRECTORY_WORKSPACE_FILE_NAME = "WORKSPACE.bzlmod"

@TestApplication
internal class BazelProjectOpenProcessorTest {

  private val directoryRoot by tempPathFixture()
  private val linkDirectory by tempPathFixture()

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

  @Test
  fun `should not open an unrelated file in the project`() {
    createWorkspaceFile()
    val readmeFile = createFileInRoot("README.md")

    bazelProjectOpenProcessor().canOpenProject(refresh(readmeFile)) shouldBe false
  }

  @Test
  fun `opens the project with the workspace file when the workspace file is opened`() {
    val workspaceFile = createWorkspaceFile()
    val projectViewFile = createFileInRoot(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)

    openedProject(workspaceFile) shouldBe (workspaceFile to projectViewFile)
  }

  @Test
  fun `opens the project with the workspace file when the default project view is opened`() {
    val workspaceFile = createWorkspaceFile()
    val projectViewFile = createFileInRoot(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)

    openedProject(projectViewFile) shouldBe (workspaceFile to projectViewFile)
  }

  @Test
  fun `opens the project with the workspace file when a build file is opened`() {
    val workspaceFile = createWorkspaceFile()
    val projectViewFile = createFileInRoot(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)
    val buildFile = createFileInRoot("BUILD.bazel")

    openedProject(buildFile) shouldBe (workspaceFile to projectViewFile)
  }

  @Test
  fun `opens the project with the workspace file when the root directory is opened`() {
    val workspaceFile = createWorkspaceFile()
    val projectViewFile = createFileInRoot(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)

    openedProject(directoryRoot) shouldBe (workspaceFile to projectViewFile)
  }

  @Test
  fun `opens the project with the first workspace file when a second workspace file is opened`() {
    val workspaceFile = createWorkspaceFile()
    val projectViewFile = createFileInRoot(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)
    val secondWorkspaceFile = createFileInRoot(Constants.WORKSPACE_FILE_NAME)

    openedProject(secondWorkspaceFile) shouldBe (workspaceFile to projectViewFile)
  }

  @Test
  fun `opens the project with a custom project view when the custom project view is opened`() {
    createWorkspaceFile()
    createFileInRoot(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)
    val projectViewFile = createFileInRoot("custom.bazelproject")

    openedProject(projectViewFile) shouldBe (projectViewFile to projectViewFile)
  }

  @Test
  fun `opens the project with the workspace file when the dot bazelbsp project view is opened`() {
    val workspaceFile = createWorkspaceFile()
    val projectViewFile = createFileInRoot("${Constants.DOT_BAZELBSP_DIR_NAME}/${Constants.DEFAULT_PROJECT_VIEW_FILE_NAME}")

    openedProject(projectViewFile) shouldBe (workspaceFile to projectViewFile)
  }

  @Test
  fun `opens the project with the root project view when the dot bazelbsp project view next to it is opened`() {
    val workspaceFile = createWorkspaceFile()
    val rootProjectViewFile = createFileInRoot(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)
    val dotBazelBspProjectViewFile = createFileInRoot("${Constants.DOT_BAZELBSP_DIR_NAME}/${Constants.DEFAULT_PROJECT_VIEW_FILE_NAME}")

    openedProject(dotBazelBspProjectViewFile) shouldBe (workspaceFile to rootProjectViewFile)
  }

  @Test
  fun `opens the project with the workspace file when the legacy default project view is opened`() {
    val workspaceFile = createWorkspaceFile()
    val projectViewFile = createFileInRoot(Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME)

    openedProject(projectViewFile) shouldBe (workspaceFile to projectViewFile)
  }

  @Test
  fun `creates the dot bazelbsp project view when the project has no project view`() {
    val workspaceFile = createWorkspaceFile()
    val projectViewFile = directoryRoot.toRealPath().resolve(Constants.DOT_BAZELBSP_DIR_NAME).resolve(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)

    openedProject(workspaceFile) shouldBe (workspaceFile to projectViewFile)
    projectViewFile.isRegularFile() shouldBe true
  }

  @Test
  fun `opens the project with the real root when a symlinked root is opened`() {
    val workspaceFile = createWorkspaceFile()
    val projectViewFile = createFileInRoot(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)
    val linkedRoot = linkDirectory.resolve("link").createSymbolicLinkPointingTo(directoryRoot.toRealPath())

    openedProject(linkedRoot.resolve(Constants.MODULE_BAZEL_FILE_NAME)) shouldBe (workspaceFile to projectViewFile)
  }

  @Test
  fun `keeps the existing dot idea directory`() {
    val workspaceFile = createWorkspaceFile()
    createFileInRoot(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)
    val dotIdeaFile = createFileInRoot("${Project.DIRECTORY_STORE_FOLDER}/kept.txt")

    openedProject(workspaceFile)

    dotIdeaFile.isRegularFile() shouldBe true
  }

  private fun openedProject(openedFile: Path): Pair<Path?, Path> = timeoutRunBlocking(2.minutes, context = Dispatchers.Default) {
    val openProjectTask = OpenProjectTask {
      beforeInitTasks += { setBazelStartupSyncEnabledInTests(it, false) }
    }
    val project = ProjectUtil.openOrImportAsync(openedFile, openProjectTask) ?: error("Cannot open $openedFile")
    project.useProjectAsync {
      val projectPath = ProjectImpl.PROJECT_PATH.get(it)
      val descriptor = it.stateStore.storeDescriptor as BazelProjectStoreDescriptor
      descriptor.projectIdentityFile.toString() shouldBe projectPath.toString()
      it.basePath shouldBe directoryRoot.toRealPath().invariantSeparatorsPathString
      awaitOpenedProjectView(it, descriptor.projectViewFile)
      projectPath to descriptor.projectViewFile
    }
  }

  private suspend fun awaitOpenedProjectView(project: Project, projectViewFile: Path) {
    val fileEditorManager = FileEditorManager.getInstance(project)
    waitUntil("The project view $projectViewFile does not open in an editor", timeout = 1.minutes) {
      fileEditorManager.openFiles.any { it.toNioPathOrNull() == projectViewFile }
    }
    withContext(Dispatchers.EDT) {
      FileEditorManagerEx.getInstanceEx(project).closeAllFiles()
    }
  }

  private fun createFileInRoot(relativePath: String): Path =
    directoryRoot.toRealPath().resolve(relativePath).createParentDirectories().apply { writeText("") }

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

  private fun createWorkspaceFile(): Path =
    directoryRoot.toRealPath().resolve(Constants.MODULE_BAZEL_FILE_NAME).createFile()

  private fun refreshDirectoryRoot(): VirtualFile = refresh(directoryRoot)

  private fun refresh(path: Path): VirtualFile =
    VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path) ?: error("Cannot refresh $path")

  private fun bazelProjectOpenProcessor(): ProjectOpenProcessor =
    ProjectOpenProcessor.EXTENSION_POINT_NAME.extensionList
      .single { it.name == BazelPluginConstants.BAZEL_DISPLAY_NAME }
}
