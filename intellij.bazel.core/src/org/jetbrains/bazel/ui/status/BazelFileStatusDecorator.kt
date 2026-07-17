package org.jetbrains.bazel.ui.status

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.ProjectViewUpdateCause
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.action.registered.openProjectView
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.ignore.BazelIgnoreService
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspace.fileEvents.BazelFileEventProcessor
import org.jetbrains.jps.model.java.JavaResourceRootType
import java.awt.Color
import java.util.function.Function
import javax.swing.JComponent

internal class BazelSourceFileEditorTabColorProvider : EditorTabColorProvider {
  override fun getEditorTabColor(project: Project, file: VirtualFile): Color? {
    if (!project.isBazelProject) return null
    if (!BazelFeatureFlags.highlightUnsyncedSourceFiles) return null
    return if (showAsUnsyncedSourceFile(project, file)) UNSYNCED_BACKGROUND_COLOR
    else null
  }
}

internal class BazelSourceFileEditorTabTitleProvider : EditorTabTitleProvider {
  private fun getEditorTabTitleImpl(project: Project, file: VirtualFile): String? {
    return if (showAsUnsyncedSourceFile(project, file)) {
      "${file.presentableName} ${BazelPluginBundle.message("sync.status.unsynced.source.file.hint")}"
    } else {
      null
    }
  }

  override suspend fun getEditorTabTitleAsync(project: Project, file: VirtualFile): String? {
    if (!project.isBazelProject) return null
    if (!BazelFeatureFlags.highlightUnsyncedSourceFiles) return null
    return readAction {
      getEditorTabTitleImpl(project, file)
    }
  }

  override fun getEditorTabTitle(project: Project, file: VirtualFile): String? {
    if (!project.isBazelProject) return null
    if (!BazelFeatureFlags.highlightUnsyncedSourceFiles) return null
    return ReadAction.computeCancellable<String, RuntimeException> {
      getEditorTabTitleImpl(project, file)
    }
  }
}

internal class BazelSourceFileNotificationProvider : EditorNotificationProvider {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isBazelProject) return null
    if (!BazelFeatureFlags.highlightUnsyncedSourceFiles) return null
    if (!showAsUnsyncedSourceFile(project, file)) return null
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
          ProjectSyncTask(project).fullSync(buildProject = false)
        }
      }
    }
  }
}

private val IGNORE_COLOR: JBColor = JBColor(Color(159, 107, 0), Color(159, 107, 0))
private val UNSYNCED_BACKGROUND_COLOR: JBColor = JBColor(Color(252, 234, 234), Color(94, 56, 56))

internal class BazelSourceFileNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (!project.isBazelProject) return
    val vFile = node.virtualFile
    if (vFile != null) {
      when {
        BazelFeatureFlags.highlightIgnoredSourceFiles && showAsIgnoredSourceFile(project, vFile) -> {
          data.forcedTextForeground = IGNORE_COLOR
        }

        BazelFeatureFlags.highlightUnsyncedSourceFiles && showAsUnsyncedSourceFile(project, vFile) -> {
          data.addText(vFile.name + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
          data.addText(BazelPluginBundle.message("sync.status.unsynced.source.file.hint"), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
          data.background = UNSYNCED_BACKGROUND_COLOR
        }
      }
    }
  }
}

@ApiStatus.Internal
fun showAsUnsyncedSourceFile(project: Project, file: VirtualFile): Boolean {
  if (file.isDirectory) return false
  val extension = file.extension ?: return false
  if (LanguageClass.fromExtension(extension) == null) return false
  try {
    if (!file.toNioPath().startsWith(project.rootDir.toNioPath())) return false
  }
  catch (_: UnsupportedOperationException) {
    return false
  }

  if (project.isSyncInProgress()) return false
  if (!BazelFileEventProcessor.getInstance(project).isIdle()) return false
  if (BazelIgnoreService.getInstance(project).isIgnored(file)) return false

  val projectFileIndex = ProjectFileIndex.getInstance(project)
  val isExcluded = projectFileIndex.isExcluded(file)
  if (isExcluded) return false

  val isResource = projectFileIndex.isUnderSourceRootOfType(file, setOf(JavaResourceRootType.RESOURCE, JavaResourceRootType.TEST_RESOURCE))
  if (isResource) return false

  val targetUtils = project.targetUtils
  if (!targetUtils.isLoaded()) return false

  return targetUtils.getTargetsForFile(file).isEmpty()
}

@ApiStatus.Internal
fun showAsIgnoredSourceFile(project: Project, file: VirtualFile): Boolean {
  if (project.isSyncInProgress()) return false
  return BazelIgnoreService.getInstance(project).isIgnored(file)
}

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class BazelFileStatusRefresher(private val project: Project) {
  init {
    project.targetUtils.onLoaded {
      refreshAllFilesPresentation()
    }

    project.messageBus.connect().subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {
          // Cleanup all "unsynced" presentations early
          refreshAllFilesPresentation()
        }

        override fun syncFinished(canceled: Boolean) {
          refreshAllFilesPresentation()
        }
      },
    )
  }

  fun refreshAllFilesPresentation() {
    BazelCoroutineService.getInstance(project).start {
      withContext(Dispatchers.EDT) {
        ProjectView.getInstance(project).refresh(ProjectViewUpdateCause.PLUGIN_BAZEL)
        EditorNotifications.getInstance(project).updateAllNotifications()
        with(FileEditorManager.getInstance(project)) {
          openFiles.forEach {
            updateFilePresentation(it)
          }
        }
      }
    }
  }

  companion object {
    fun getInstance(project: Project): BazelFileStatusRefresher = project.service()
  }
}
