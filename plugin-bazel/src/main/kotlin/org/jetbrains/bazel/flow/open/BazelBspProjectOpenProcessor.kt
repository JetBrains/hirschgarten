package org.jetbrains.bazel.flow.open

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.settings.bazelProjectSettings
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.impl.flow.open.BaseBspProjectOpenProcessor
import org.jetbrains.plugins.bsp.impl.flow.open.BspProjectOpenProcessor
import org.jetbrains.plugins.bsp.impl.flow.open.BspProjectOpenProcessorExtension
import org.jetbrains.plugins.bsp.impl.flow.open.toBuildToolId
import javax.swing.Icon

internal class BazelBspProjectOpenProcessor : BaseBspProjectOpenProcessor(bazelBspBuildToolId) {
  override fun calculateProjectFolderToOpen(virtualFile: VirtualFile): VirtualFile =
    when {
      virtualFile.isProjectViewFile() -> findProjectFolderFromProjectViewFile(virtualFile)
      virtualFile.isWorkspaceFile() -> virtualFile.parent
      virtualFile.isBazelBspConnectionFile() -> BspProjectOpenProcessor().calculateProjectFolderToOpen(virtualFile)
      else -> null
    } ?: error("Cannot find the suitable Bazel project folder to open for the given file $virtualFile.")

  private tailrec fun findProjectFolderFromProjectViewFile(vFile: VirtualFile?): VirtualFile? =
    when {
      vFile == null -> null
      vFile.isDirectory && canOpenProject(vFile) -> vFile
      else -> findProjectFolderFromProjectViewFile(vFile.parent)
    }

  private fun VirtualFile.isWorkspaceFile() = isFile && name in BazelPluginConstants.WORKSPACE_FILE_NAMES

  override val icon: Icon = BazelPluginIcons.bazel

  override val name: String = "Bazel"

  override val isStrongProjectInfoHolder: Boolean
    get() = ApplicationManager.getApplication().isHeadlessEnvironment

  override fun canOpenProject(file: VirtualFile): Boolean =
    when {
      file.isFile -> file.isBazelBspConnectionFile() || file.isProjectViewFile()
      else -> file.children.any { it.name in BazelPluginConstants.WORKSPACE_FILE_NAMES }
    }

  private fun VirtualFile.isBazelBspConnectionFile() = toBuildToolId() == bazelBspBuildToolId

  override fun calculateBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit {
    if (originalVFile.isBazelBspConnectionFile()) {
      return BspProjectOpenProcessor().calculateBeforeOpenCallback(originalVFile)
    }
    if (originalVFile.isProjectViewFile()) {
      return { project ->
        project.bazelProjectSettings =
          project.bazelProjectSettings.withNewProjectViewPath(originalVFile.toNioPath().toAbsolutePath())
      }
    }
    return {}
  }

  private fun VirtualFile.isProjectViewFile() = extension == BazelPluginConstants.PROJECT_VIEW_FILE_EXTENSION
}

internal class BazelBspProjectOpenProcessorExtension : BspProjectOpenProcessorExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val shouldBspProjectOpenProcessorBeAvailable: Boolean = false
}
