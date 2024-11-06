package org.jetbrains.bazel.flow.open

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.bsp.connection.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.settings.bazelProjectSettings
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.impl.flow.open.BaseBspProjectOpenProcessor
import org.jetbrains.plugins.bsp.impl.flow.open.BspProjectOpenProcessor
import org.jetbrains.plugins.bsp.impl.flow.open.BspProjectOpenProcessorExtension
import org.jetbrains.plugins.bsp.impl.flow.open.toBuildToolId
import java.io.File
import javax.swing.Icon
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText

private val INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE =
  """
  derive_targets_from_directories: true
  directories: %s
  """.trimIndent()

internal class BazelBspProjectOpenProcessor : BaseBspProjectOpenProcessor(bazelBspBuildToolId) {
  override fun calculateProjectFolderToOpen(virtualFile: VirtualFile): VirtualFile =
    when {
      virtualFile.isBazelBspConnectionFile() -> BspProjectOpenProcessor().calculateProjectFolderToOpen(virtualFile)
      else -> findProjectFolderFromEligibleFile(virtualFile)
    } ?: error("Cannot find the suitable Bazel project folder to open for the given file $virtualFile.")

  private tailrec fun findProjectFolderFromEligibleFile(vFile: VirtualFile?): VirtualFile? =
    when {
      vFile == null -> null
      vFile.containsWorkspaceFile() -> vFile
      else -> findProjectFolderFromEligibleFile(vFile.parent)
    }

  private fun VirtualFile.containsWorkspaceFile() = isDirectory && children?.any { it.isWorkspaceFile() } == true

  private fun VirtualFile.isWorkspaceFile() = isFile && name in BazelPluginConstants.WORKSPACE_FILE_NAMES

  private fun VirtualFile.isBuildFile() = isFile && name in BazelPluginConstants.BUILD_FILE_NAMES

  override val icon: Icon = BazelPluginIcons.bazel

  override val name: String = "Bazel"

  override val isStrongProjectInfoHolder: Boolean
    get() = ApplicationManager.getApplication().isHeadlessEnvironment

  override fun canOpenProject(file: VirtualFile): Boolean =
    when {
      file.isEligibleFile() -> true
      else -> file.children?.any { it.isEligibleFile() } == true
    }

  private fun VirtualFile.isEligibleFile() = isBazelBspConnectionFile() || isWorkspaceFile() || isBuildFile()

  private fun VirtualFile.isBazelBspConnectionFile() = toBuildToolId() == bazelBspBuildToolId

  override fun calculateBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit =
    when {
      originalVFile.isBazelBspConnectionFile() -> BspProjectOpenProcessor().calculateBeforeOpenCallback(originalVFile)
      originalVFile.isProjectViewFile() -> projectViewFileBeforeOpenCallback(originalVFile)
      originalVFile.isBuildFile() -> buildFileBeforeOpenCallback(originalVFile)
      else -> {
        val buildFile = calculateBuildFileFromDirectory(originalVFile)
        buildFile?.let { buildFileBeforeOpenCallback(it) } ?: {}
      }
    }

  private fun calculateBuildFileFromDirectory(originalVFile: VirtualFile): VirtualFile? =
    originalVFile.children?.firstOrNull { it.isBuildFile() }

  private fun buildFileBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit =
    fun(project) {
      val projectRoot = project.rootDir
      val bazelPackageDir = originalVFile.parent ?: return
      val relativePath = calculateRelativePathForInferredDirectory(projectRoot, bazelPackageDir)
      val outputProjectViewFilePath = bazelPackageDir.toNioPath().resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)
      if (!outputProjectViewFilePath.isRegularFile()) {
        outputProjectViewFilePath.deleteIfExists()
        outputProjectViewFilePath.writeText(INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE.format(relativePath))
      }
      project.bazelProjectSettings =
        project.bazelProjectSettings.withNewProjectViewPath(outputProjectViewFilePath.toAbsolutePath())
    }

  private fun calculateRelativePathForInferredDirectory(projectRoot: VirtualFile, bazelPackageDir: VirtualFile): String {
    val relativePath = VfsUtil.findRelativePath(projectRoot, bazelPackageDir, File.separatorChar)
    if (relativePath.isNullOrBlank()) return "."
    return relativePath
  }

  private fun projectViewFileBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit =
    { project ->
      project.bazelProjectSettings =
        project.bazelProjectSettings.withNewProjectViewPath(originalVFile.toNioPath().toAbsolutePath())
    }

  private fun VirtualFile.isProjectViewFile() = extension == BazelPluginConstants.PROJECT_VIEW_FILE_EXTENSION
}

internal class BazelBspProjectOpenProcessorExtension : BspProjectOpenProcessorExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val shouldBspProjectOpenProcessorBeAvailable: Boolean = false
}
