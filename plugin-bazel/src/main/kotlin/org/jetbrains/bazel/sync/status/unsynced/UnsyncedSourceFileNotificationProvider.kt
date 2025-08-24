package org.jetbrains.bazel.sync.status.unsynced

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import org.jetbrains.bazel.action.registered.openProjectView
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import java.util.function.Function
import javax.swing.JComponent

class UnsyncedSourceFileNotificationProvider : EditorNotificationProvider {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isBazelProject) return null
    if (project.isSyncInProgress()) return null
    if (!file.isUnsyncedSourceFile(project)) return null
    return Function { UnsyncedSourceFileEditorPanel(project, it) }
  }

  private class UnsyncedSourceFileEditorPanel(project: Project, fileEditor: FileEditor) :
    EditorNotificationPanel(fileEditor, Status.Warning) {
    init {
      text = BazelPluginBundle.message("sync.status.unsynced.source.file.notification")
      createActionLabel(
        BazelPluginBundle.message("widget.config.file.popup.message", BazelPluginBundle.message("tool.window.generic.config.file")),
      ) {
        BazelCoroutineService.getInstance(project).start {
          openProjectView(project)
        }
      }
      createActionLabel(BazelPluginBundle.message("resync.action.text")) {
        BazelCoroutineService.getInstance(project).start {
          ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
        }
      }
    }
  }
}
