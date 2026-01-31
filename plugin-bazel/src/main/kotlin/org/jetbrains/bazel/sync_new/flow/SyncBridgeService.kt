package org.jetbrains.bazel.sync_new.flow

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.sync.status.SyncAlreadyInProgressException
import org.jetbrains.bazel.sync.status.SyncFatalFailureException
import org.jetbrains.bazel.sync.status.SyncPartialFailureException
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bazel.ui.console.ids.PROJECT_SYNC_TASK_ID
import org.jetbrains.bazel.ui.console.syncConsole
import java.util.concurrent.CancellationException

@Service(Service.Level.PROJECT)
class SyncBridgeService(
  private val project: Project,
) {
  private val executor by lazy {
    SyncExecutor(
      project = project,
    )
  }

  suspend fun sync(spec: SyncSpec, scope: SyncScope) {
    withConsole(
      redo = { sync(spec, scope) },
      after = {
        SyncStatusService.getInstance(project).finishSync()
        withContext(Dispatchers.EDT) {
          ProjectView.getInstance(project).refresh()
        }
      },
      action = {
        withContext(Dispatchers.EDT) {
          FileDocumentManager.getInstance().saveAllDocuments()
        }
        project.serviceAsync<ProjectViewService>().forceReparseCurrentProjectViewFiles()
        project.serviceAsync<SyncStatusService>().startSync()
        val result = executor.execute(spec, scope)
        when (result) {
          SyncStatus.Failure -> throw SyncFatalFailureException()
          SyncStatus.PartialFailure -> throw SyncPartialFailureException()
          else -> {}
        }
      },
    )
  }

  private suspend fun withConsole(
    redo: suspend () -> Unit,
    after: suspend () -> Unit,
    action: suspend () -> Unit,
  ) {
    coroutineScope {
      var syncAlreadyInProgress = false
      try {
        project.syncConsole.startTask(
          taskId = PROJECT_SYNC_TASK_ID,
          title = BazelPluginBundle.message("console.task.sync.title"),
          message = BazelPluginBundle.message("console.task.sync.in.progress"),
          cancelAction = {
            SyncStatusService.getInstance(project).cancel()
            coroutineContext.cancel()
          },
          redoAction = { redo() },
        )

        action()
        project.syncConsole.finishTask(PROJECT_SYNC_TASK_ID, BazelPluginBundle.message("console.task.sync.success"))
      } catch (e: CancellationException) {
        project.syncConsole.finishTask(
          PROJECT_SYNC_TASK_ID,
          BazelPluginBundle.message("console.task.sync.cancelled"),
          SkippedResultImpl(),
        )
        throw e
      } catch (_: SyncAlreadyInProgressException) {
        syncAlreadyInProgress = true
      } catch (_: SyncPartialFailureException) {
        project.syncConsole.addWarnMessage(
          PROJECT_SYNC_TASK_ID,
          BazelPluginBundle.message("console.task.sync.partialsuccess"),
        )
        project.syncConsole.finishTask(
          PROJECT_SYNC_TASK_ID,
          BazelPluginBundle.message("console.task.sync.partialsuccess"),
          SuccessResultImpl(true),
        )
      } catch (_: SyncFatalFailureException) {
        project.syncConsole.finishTask(
          PROJECT_SYNC_TASK_ID,
          BazelPluginBundle.message("console.task.sync.fatalfailure"),
          FailureResultImpl(),
        )
      } catch (e: Exception) {
        project.syncConsole.finishTask(
          PROJECT_SYNC_TASK_ID,
          BazelPluginBundle.message("console.task.sync.failed"),
          FailureResultImpl(e),
        )
      } finally {
        if (!syncAlreadyInProgress) {
          after()
        }
      }
    }
  }
}
