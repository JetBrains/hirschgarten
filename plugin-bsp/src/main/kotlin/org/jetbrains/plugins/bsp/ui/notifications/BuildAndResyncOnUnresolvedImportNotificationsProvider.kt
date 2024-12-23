package org.jetbrains.plugins.bsp.ui.notifications

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.IncompleteDependenciesService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.isJavaFileType
import org.jetbrains.kotlin.idea.util.isKotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncTask
import org.jetbrains.plugins.bsp.impl.flow.sync.SecondPhaseSync
import org.jetbrains.plugins.bsp.impl.projectAware.isSyncInProgress
import java.util.function.Function
import javax.swing.JComponent

class BuildAndResyncOnUnresolvedImportNotificationsProvider : EditorNotificationProvider {
  private val disableNotificationForFile = mutableSetOf<VirtualFile>()

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isBspProject) return null
    if (file in disableNotificationForFile) return null
    if (project.isSyncInProgress()) return null
    if (BspFeatureFlags.isBuildProjectOnSyncEnabled) return null
    if (!project.service<IncompleteDependenciesService>().getState().isComplete) return null

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
    return when (psiFile) {
      is KtFile -> hasUnresolvedImport(psiFile)
      is PsiJavaFile -> hasUnresolvedImport(psiFile)
      else -> false
    }
  }

  private fun hasUnresolvedImport(psiFile: KtFile): Boolean {
    val importList = psiFile.importList ?: return false
    return importList.imports
      .mapNotNull {
        it.importedReference?.getQualifiedElementSelector()?.mainReference
      }.any { reference ->
        reference.multiResolve(false).isEmpty()
      }
  }

  private fun hasUnresolvedImport(psiFile: PsiJavaFile): Boolean {
    val importList = psiFile.importList ?: return false
    return importList.allImportStatements
      .mapNotNull {
        it.reference as? PsiPolyVariantReference
      }.any { reference ->
        reference.multiResolve(false).isEmpty()
      }
  }

  private inner class BuildAndResyncOnUnresolvedImportEditorPanel(project: Project, fileEditor: FileEditor) :
    EditorNotificationPanel(fileEditor, Status.Warning) {
    init {
      text = BspPluginBundle.message("notification.unresolved.imports")

      createActionLabel(BspPluginBundle.message("build.and.resync.action.text")) {
        BspCoroutineService.getInstance(project).start {
          ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = true)
        }
      }
      val virtualFile = fileEditor.file
      setCloseAction {
        disableNotificationForFile.add(virtualFile)
        EditorNotifications.getInstance(project).updateNotifications(this@BuildAndResyncOnUnresolvedImportNotificationsProvider)
      }
    }
  }
}
