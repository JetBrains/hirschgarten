package org.jetbrains.bazel.ui.notifications

import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IncompleteDependenciesService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import java.util.function.Function
import javax.swing.JComponent

class BuildAndResyncOnUnresolvedImportNotificationsProvider : EditorNotificationProvider {
  private val disableNotificationForFile = mutableSetOf<VirtualFile>()

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isBazelProject) return null
    if (file in disableNotificationForFile) return null
    if (project.isSyncInProgress()) return null
    if (BazelFeatureFlags.isBuildProjectOnSyncEnabled) return null
    if (!project.service<IncompleteDependenciesService>().getState().isComplete) return null

    val dumbService = DumbService.getInstance(project)
    if (dumbService.isDumb) {
      dumbService.runWhenSmart {
        EditorNotifications.getInstance(project).updateAllNotifications()
      }
      return null
    }

    if (!hasUnresolvedImport(project, file)) return null

    return Function { editor ->
      BuildAndResyncOnUnresolvedImportEditorPanel(project, editor)
    }
  }

  private fun hasUnresolvedImport(project: Project, file: VirtualFile): Boolean {
    // TODO Scala
    if (!file.isKotlinFileType() && !file.isJavaFileType()) return false
    if (!ProjectFileIndex.getInstance(project).isInSourceContent(file)) return false
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return false

    return UnresolvedImportChecker.ep.extensionList.any { it.hasUnresolvedImport(project, psiFile) }
  }

  private fun VirtualFile.isKotlinFileType(): Boolean = extension == "kt"

  private fun VirtualFile.isJavaFileType(): Boolean = extension == "java"

  private inner class BuildAndResyncOnUnresolvedImportEditorPanel(project: Project, fileEditor: FileEditor) :
    EditorNotificationPanel(fileEditor, Status.Warning) {
    init {
      text = BazelPluginBundle.message("notification.unresolved.imports")

      createActionLabel(BazelPluginBundle.message("build.and.resync.action.text")) {
        BazelCoroutineService.getInstance(project).start {
          ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = true)
        }
      }
      val virtualFile = fileEditor.file
      setCloseAction {
        disableNotificationForFile.add(virtualFile)
        EditorNotifications.getInstance(project).updateAllNotifications()
      }
    }
  }

  class NotificationUpdateSyncHook : ProjectPostSyncHook {
    override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
      EditorNotifications.getInstance(environment.project).updateAllNotifications()
    }
  }
}

interface UnresolvedImportChecker {
  fun hasUnresolvedImport(project: Project, psiFile: PsiFile): Boolean

  companion object {
    val ep = ExtensionPointName.create<UnresolvedImportChecker>("org.jetbrains.bazel.unresolvedImportChecker")
  }
}
