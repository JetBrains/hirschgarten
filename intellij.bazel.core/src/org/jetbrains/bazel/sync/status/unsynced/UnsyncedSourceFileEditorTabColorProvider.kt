package org.jetbrains.bazel.sync.status.unsynced

import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.status.isSyncInProgress
import java.awt.Color

private class UnsyncedSourceFileEditorTabColorProvider : EditorTabColorProvider {
  override fun getEditorTabColor(project: Project, file: VirtualFile): Color? {
    if (!project.isBazelProject) return null
    if (project.isSyncInProgress()) return null
    return if (file.isUnsyncedSourceFile(project)) UNSYNCED_COLOR else null
  }
}

private class UnsyncedSourceFileEditorTabTitleProvider : EditorTabTitleProvider {
  override fun getEditorTabTitle(project: Project, file: VirtualFile): String? {
    if (!project.isBazelProject) return null
    if (project.isSyncInProgress()) return null
    return if (file.isUnsyncedSourceFile(
        project,
      )
    ) {
      "${file.presentableName} ${BazelPluginBundle.message("sync.status.unsynced.source.file.hint")}"
    } else {
      null
    }
  }
}
