package org.jetbrains.bazel.clion.debug

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NonNls
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot

private val LOG = logger<BazelDebugAction>()

private val BANNER = "#".repeat(80)

/**
 * Base class for an action that reports a part of the plugin state.
 *
 * The action renders the value that [exec] returns as JSON. It writes the JSON
 * to the log, and it opens it in a read-only editor tab when [showOutputInEditor]
 * is `true`. Call [fail] to report a reason as an `error` field instead of an
 * exception.
 */
internal abstract class BazelDebugAction : DumbAwareAction() {

  final override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  final override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabledAndVisible = project != null && project.isBazelProject
  }

  final override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val title: @NlsSafe String = e.presentation.text ?: javaClass.simpleName

    e.coroutineScope.launch(Dispatchers.Default) {
      withBackgroundProgress(project, title) {
        report(project)
      }
    }
  }

  private suspend fun report(project: Project) {
    val name = javaClass.simpleName

    val content = try {
      exec(project)
    }
    catch (ex: DebugActionFailed) {
      mapOf("error" to ex.message)
    }

    val json = bazelDebugGson.toJson(content)

    LOG.info(BANNER)
    LOG.info(String.format("# %-76s #", name))
    LOG.info(BANNER)
    for (line in json.lines()) {
      LOG.info(line)
    }
    LOG.info(BANNER)

    if (showOutputInEditor) {
      openInEditor(project, "$name.json", json)
    }
  }

  private suspend fun openInEditor(
    project: Project,
    name: @NlsSafe String,
    text: @NonNls String,
  ) {
    withContext(Dispatchers.EDT) {
      val file = ReadOnlyLightVirtualFile(name, PlainTextLanguage.INSTANCE, text)
      FileEditorManager.getInstance(project).openFile(file, false)
    }
  }

  protected suspend fun snapshotOrFail(project: Project): WorkspaceSnapshot {
    val snapshot = project.service<WorkspaceSnapshotService>().currentSnapshot()
    if (snapshot == WorkspaceSnapshot.EMPTY) {
      fail("no workspace snapshot found, the project is not synced")
    }

    return snapshot
  }

  protected fun fail(reason: @NonNls String): Nothing = throw DebugActionFailed(reason)

  protected open val showOutputInEditor: Boolean
    get() = ApplicationManager.getApplication().isInternal

  /** Returns a tree of plain values. The base class renders it as pretty JSON. */
  protected abstract suspend fun exec(project: Project): Any?
}

private class DebugActionFailed(reason: @NonNls String) : Exception(reason)
