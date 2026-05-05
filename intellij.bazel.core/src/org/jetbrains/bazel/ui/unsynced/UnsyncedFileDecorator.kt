package org.jetbrains.bazel.ui.unsynced

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.bazel.action.registered.openProjectView
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.target.targetUtils
import java.awt.Color
import java.util.function.Function
import javax.swing.JComponent

internal class UnsyncedSourceFileEditorTabColorProvider : EditorTabColorProvider {
  override fun getEditorTabColor(project: Project, file: VirtualFile): Color? {
    if (!project.isBazelProject) return null
    if (project.isSyncInProgress()) return null
    if (!BazelFeatureFlags.highlightUnsyncedSourceFiles) return null
    return if (file.isUnsyncedSourceFile(project)) UNSYNCED_COLOR else null
  }
}

internal class UnsyncedSourceFileEditorTabTitleProvider : EditorTabTitleProvider {
  override fun getEditorTabTitle(project: Project, file: VirtualFile): String? {
    if (!project.isBazelProject) return null
    if (project.isSyncInProgress()) return null
    if (!BazelFeatureFlags.highlightUnsyncedSourceFiles) return null
    return if (file.isUnsyncedSourceFile(project)) {
      "${file.presentableName} ${BazelPluginBundle.message("sync.status.unsynced.source.file.hint")}"
    } else {
      null
    }
  }
}

internal class UnsyncedSourceFileNotificationProvider : EditorNotificationProvider {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isBazelProject) return null
    if (project.isSyncInProgress()) return null
    if (!BazelFeatureFlags.highlightUnsyncedSourceFiles) return null
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

private val UNSYNCED_COLOR: JBColor = JBColor(Color(252, 234, 234), Color(94, 56, 56))

internal class UnsyncedSourceFileNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (!project.isBazelProject) return
    if (project.isSyncInProgress()) return
    if (!BazelFeatureFlags.highlightUnsyncedSourceFiles) return
    val vFile = node.virtualFile ?: return
    if (vFile.isUnsyncedSourceFile(project)) {
      data.addText(vFile.name + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
      data.addText(BazelPluginBundle.message("sync.status.unsynced.source.file.hint"), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
      data.background = UNSYNCED_COLOR
    }
  }
}

private fun VirtualFile.isUnsyncedSourceFile(project: Project): Boolean {
  val extension = extension ?: return false
  if (LanguageClass.fromExtension(extension) == null) return false
  try {
    if (!toNioPath().startsWith(project.rootDir.toNioPath())) return false
  }
  catch (e: UnsupportedOperationException) {
    return false
  }
  val targetUtils = project.targetUtils
  return targetUtils.getTargetsForFile(this).isEmpty()
}
