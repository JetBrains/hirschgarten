package org.jetbrains.bazel.clion.run.debug

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.cidr.debugger.profiles.CidrDebugProfileManager
import com.intellij.execution.ExecutionException
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.AsyncProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.EDT
import com.intellij.util.asSafely
import com.jetbrains.cidr.cpp.execution.DebugProfileErrorWithCustomNotification
import com.jetbrains.cidr.cpp.execution.showMissingDebugProfileNotification
import com.jetbrains.cidr.execution.CidrRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.bazel.clion.BazelCLionCoreBundle
import org.jetbrains.bazel.clion.run.CcRunHandler
import org.jetbrains.bazel.clion.run.CcTestHandler
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.ShowConsole
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.task.BazelBuildTaskListener
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.toPromiseWithoutLogError
import kotlin.random.Random

class CcDebugRunner : AsyncProgramRunner<RunnerSettings>() {

  override fun getRunnerId(): @NonNls String = "CcDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return executorId == DefaultDebugExecutor.EXECUTOR_ID &&
           profile is BazelRunConfiguration &&
           profile.targets.size == 1 &&
           (profile.handler is CcRunHandler || profile.handler is CcTestHandler) &&
           // the debug session needs the workspace snapshot, which the sync replaces
           !profile.project.isSyncInProgress()
  }

  @Throws(ExecutionException::class)
  override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
    val ccState = state.asSafely<CcDebugCommandLineState>()
                  ?: throw ExecutionException(BazelCLionCoreBundle.message("debug.error.invalid.state"))

    val label = BazelRunConfiguration.get(environment).targets.singleOrNull()
                ?: throw ExecutionException(BazelCLionCoreBundle.message("debug.error.no.single.target"))

    val debugProfile = CidrDebugProfileManager.getInstance().getCurrentDebugProfile(environment.project)
    if (debugProfile == null) {
      showMissingDebugProfileNotification(environment.project)
      throw DebugProfileErrorWithCustomNotification()
    }
    ccState.debugProfile = debugProfile

    val console = ConsoleService.getInstance(environment.project).buildConsole
    val taskGroupId = TaskGroupId("cc-debug-${Random.nextBytes(8).toHexString()}")
    val taskId = taskGroupId.task("debug")

    val taskEvents = BazelTaskEventsService.getInstance(environment.project)
    taskEvents.saveListener(taskGroupId, BazelBuildTaskListener(console))

    val deferred = BazelCoroutineService.getInstance(environment.project).startAsync {
      prepareDebugSession(environment, ccState, label, console, taskId)

      withContext(Dispatchers.EDT) {
        CidrRunner.startDebugDescriptor(ccState, environment, false)
      }
    }

    console.startTask(
      taskId = taskId,
      title = BazelCLionCoreBundle.message("debug.task.title"),
      message = BazelCLionCoreBundle.message("debug.task.message", environment.runProfile.name),
      cancelAction = { deferred.cancel() },
      showConsole = ShowConsole.ON_FAIL,
    )

    val promise = deferred.toPromiseWithoutLogError()
    promise.onSuccess {
      console.finishTask(taskId, BazelCLionCoreBundle.message("debug.task.status.done"))
      taskEvents.removeListener(taskGroupId)
    }
    promise.onError {
      when {
        // the user can cancel in the console or in the debug info dialog
        deferred.isCancelled || it is RunCanceledByUserException -> {
          console.finishTask(taskId, BazelCLionCoreBundle.message("debug.task.status.canceled"))
        }
        else -> {
          console.finishTask(taskId, BazelCLionCoreBundle.message("debug.task.status.failed"), FailureResultImpl(it))
        }
      }
      taskEvents.removeListener(taskGroupId)
    }

    return promise
  }
}

@Throws(ExecutionException::class)
private suspend fun prepareDebugSession(
  environment: ExecutionEnvironment,
  state: CcDebugCommandLineState,
  label: Label,
  console: TaskConsole,
  taskId: TaskId,
) {
  suspend fun <T> subtask(
    key: @PropertyKey(resourceBundle = BazelCLionCoreBundle.BUNDLE_FQN) String,
    body: suspend context(CcDebugContext) () -> T,
  ): T {
    // the retry runs the same subtasks again, thus each run needs its own id
    return console.withSubtask(taskId.uniqueSubTask(key), BazelCLionCoreBundle.message(key)) { subtaskId ->
      val ctx = CcDebugContext.create(subtaskId, console, environment.project, label, state.bazelState)
      body(ctx)
    }
  }

  suspend fun checkBuild(build: CcDebugBuildResult): CcDebugInfoCheck.Result? {
    if (build.targets.isEmpty()) return null
    return subtask("debug.task.check.debug.info") { checkCcDebugInfo(build.targets) }
  }

  // the build comes first, because the debug aspect reports the configuration of the target
  var build = subtask("debug.task.discover.execution.environment") { buildExecutable() }
  var targetInfo = subtask("debug.task.discover.target") { collectTargetInfo(build.targets, reportUnknownConfiguration = true) }

  val decision = resolveDebugInfoIssues(environment.project, label, checkBuild(build), allowRetry = true)

  if (decision == CcDebugInfoDecision.RETRY_WITH_DBG) {
    build = subtask("debug.task.rebuild") { setDbgCompilationMode(); buildExecutable() }

    // the last sync did not use dbg, thus the snapshot usually does not contain the configuration of the new build
    targetInfo = subtask("debug.task.discover.target") { collectTargetInfo(build.targets, reportUnknownConfiguration = false) }

    resolveDebugInfoIssues(environment.project, label, checkBuild(build), allowRetry = false)
  }

  state.targetInfo = targetInfo
  state.executionInfo = build.executable
}
