package org.jetbrains.bazel.ui.notifications

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IncompleteDependenciesService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import javax.swing.JComponent

internal class BuildAndResyncOnUnresolvedImportNotificationsProvider :
  EditorNotificationProvider,
  DumbAware {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isBazelProject) return null
    if (file in project.service<ClosedNotifications>().files) return null
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
    val checker = UnresolvedImportChecker.ep.findFirstSafe { it.isApplicable(project, file) } ?: return false
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return false
    return checker.hasUnresolvedImport(project, psiFile)
  }

  private class BuildAndResyncOnUnresolvedImportEditorPanel(project: Project, fileEditor: FileEditor) :
    EditorNotificationPanel(fileEditor, Status.Warning) {
    init {
      text = BazelPluginBundle.message("notification.unresolved.imports")

      createActionLabel(BazelPluginBundle.message("build.and.resync.action.text")) {
        BazelCoroutineService.getInstance(project).start {
          project.service<ProjectSyncService>().sync(ProjectSyncScope.Full(build = true, phased = false))
        }
      }
      val virtualFile = fileEditor.file
      setCloseAction {
        project.service<ClosedNotifications>().files.add(virtualFile)
        EditorNotifications.getInstance(project).updateAllNotifications()
      }
    }
  }

  @Service(Service.Level.PROJECT)
  private class ClosedNotifications {
    val files: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet()
  }

  class NotificationUpdateSyncHook : ProjectPostSyncHook {
    override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
      EditorNotifications.getInstance(environment.project).updateAllNotifications()
    }
  }
}
