package org.jetbrains.bazel.ui.tabs

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileUtils
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

class BazelEditorTabTitleProvider : EditorTabTitleProvider {
  override fun getEditorTabTitle(project: Project, file: VirtualFile): String? =
    requiresDecoration(project, file.name).ifTrue {
      BazelFileUtils.getContainingDirectoryPresentablePath(project, file)
    }

  private fun requiresDecoration(project: Project, fileName: String): Boolean =
    project.isBazelProject && (fileName in BazelPluginConstants.BUILD_FILE_NAMES || fileName in BazelPluginConstants.WORKSPACE_FILE_NAMES)
}