package org.jetbrains.bazel.ui.tabs

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileUtils

class BazelEditorTabTitleProvider : EditorTabTitleProvider {
  override fun getEditorTabTitle(project: Project, file: VirtualFile): String? =
    if (requiresDecoration(project, file.name)) {
      BazelFileUtils.getContainingDirectoryPresentablePath(project, file)
    } else {
      null
    }

  private fun requiresDecoration(project: Project, fileName: String): Boolean =
    project.isBazelProject && (fileName in Constants.BUILD_FILE_NAMES || fileName in Constants.WORKSPACE_FILE_NAMES)
}
