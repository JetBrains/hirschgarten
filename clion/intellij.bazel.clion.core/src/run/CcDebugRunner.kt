package org.jetbrains.bazel.clion.run

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.AsyncProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.util.asSafely
import com.jetbrains.cidr.execution.CidrRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.clion.BazelCLionCoreBundle
import org.jetbrains.bazel.clion.workspace.CcCompilerInfo
import org.jetbrains.bazel.clion.workspace.CcTargetUtils
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.utils.ExecutableInfo
import org.jetbrains.bazel.utils.RunfileManifestOnlyException
import org.jetbrains.bazel.utils.UnexpectedScriptContentException
import org.jetbrains.bazel.languages.projectview.debugFlags
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.ShowConsole
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.run.commandLine.parseAsProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.run.task.BazelBuildTaskListener
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.server.tasks.ScriptPathBuildTargetTask
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.allTargets
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.toPromiseWithoutLogError
import java.io.IOException
import java.nio.file.Files
import kotlin.random.Random

class CcDebugRunner : AsyncProgramRunner<RunnerSettings>() {

  override fun getRunnerId(): @NonNls String = "CcDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return executorId == DefaultDebugExecutor.EXECUTOR_ID &&
      profile is BazelRunConfiguration &&
      profile.targets.size == 1 &&
      profile.handler is CcRunHandler
  }

  @Throws(ExecutionException::class)
  override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
    val ccState = state.asSafely<CcDebugCommandLineState>()
      ?: throw ExecutionException(BazelCLionCoreBundle.message("debug.error.invalid.state"))

    val console = ConsoleService.getInstance(environment.project).buildConsole
    val taskGroupId = TaskGroupId("cc-debug-${Random.nextBytes(8).toHexString()}")
    val taskId = taskGroupId.task("debug")

    val taskEvents = BazelTaskEventsService.getInstance(environment.project)
    taskEvents.saveListener(taskGroupId, BazelBuildTaskListener(console))

    val deferred = BazelCoroutineService.getInstance(environment.project).startAsync {
      prepareDebugSession(environment, ccState, console, taskId)
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
      console.finishTask(taskId, BazelCLionCoreBundle.message("debug.task.status.failed"), FailureResultImpl(it))
      taskEvents.removeListener(taskGroupId)
    }

    return promise
  }
}

private data class CcDebugContext(
  val taskId: TaskId,
  val console: TaskConsole,
  val environment: ExecutionEnvironment,
  val state: GenericRunState,
)

@Throws(ExecutionException::class)
private suspend fun prepareDebugSession(
  environment: ExecutionEnvironment,
  state: CcDebugCommandLineState,
  console: TaskConsole,
  taskId: TaskId,
) {

  suspend fun <T> subTask(
    key: @PropertyKey(resourceBundle = BazelCLionCoreBundle.BUNDLE_FQN) String,
    body: suspend context(CcDebugContext) () -> T,
  ): T {
    return console.withSubtask(taskId.subTask(key), BazelCLionCoreBundle.message(key)) { taskId ->
      val taskCtx = CcDebugContext(taskId, console, environment, state.bazelState)
      body(taskCtx)
    }
  }

  val targetInfo = subTask("debug.task.discover.target") { collectDebugTargetInfo() }

  // TODO: check if target is debuggable

  state.executionInfo = subTask("debug.task.discover.execution.environment") { collectExecutableInfo(targetInfo) }
}

data class CcDebugTargetInfo(
  val target: BuildTarget,
  val compiler: CcCompilerInfo,
)

@Throws(ExecutionException::class)
context(ctx: CcDebugContext)
private suspend fun collectDebugTargetInfo(): CcDebugTargetInfo {
  val label = BazelRunConfiguration.get(ctx.environment).targets.singleOrNull()
    ?: throw ExecutionException(BazelCLionCoreBundle.message("debug.error.no.single.target"))

  // TODO: discover target configuration i.e. aquery

  val snapshot = ctx.environment.project.service<WorkspaceSnapshotService>().currentSnapshot()
  val target = snapshot.allTargets.firstOrNull { it.key.label == label }
    ?: throw ExecutionException(BazelCLionCoreBundle.message("debug.error.target.not.found", label))

  val toolchain = CcTargetUtils.findToolchain(snapshot, target)
    ?: throw ExecutionException(BazelCLionCoreBundle.message("debug.error.no.compiler", label))
  val compilerInfo = CcTargetUtils.findToolchainCompiler(ctx.environment.project, toolchain)
    ?: throw ExecutionException(BazelCLionCoreBundle.message("debug.error.no.compiler.info", label))

  return CcDebugTargetInfo(target, compilerInfo)
}

@Throws(ExecutionException::class)
context(ctx: CcDebugContext)
private suspend fun collectExecutableInfo(targetInfo: CcDebugTargetInfo): ExecutableInfo {
  val scriptPath = try {
    withContext(Dispatchers.IO) {
      Files.createTempFile("bazel-cc-debug-", ".sh")
    }
  }
  catch (e: IOException) {
    throw ExecutionException(BazelCLionCoreBundle.message("debug.error.script.create.failed", e.message.orEmpty()))
  }

  return try {
    val status = ctx.environment.project.connection.runWithServer(ctx.taskId) { server ->

      val bazelParams = buildList {
        addAll(ScriptPathBuildTargetTask.scriptPathParams(scriptPath))
        addAll(server.projectView.debugFlags)
        addAll(parseAsProgramArguments(ctx.state.additionalBazelParams))
      }

      val params = RunParams(
        taskId = ctx.taskId,
        target = targetInfo.target.key.label,
        additionalBazelParams = bazelParams,
        checkVisibility = true,
        environmentVariables = ctx.state.env.envs,
        arguments = parseAsProgramArguments(ctx.state.programArguments),
      )

      saveAllFiles()
      server.buildTargetRun(params).statusCode
    }
    if (status != BazelStatus.SUCCESS) throw ExecutionException(BazelCLionCoreBundle.message("debug.error.script.generate.failed", status))

    val content = withContext(Dispatchers.IO) {
      Files.readString(scriptPath)
    }

    ExecutableInfo.fromBazelRunScript(content)
  }
  catch (e: IOException) {
    throw ExecutionException(BazelCLionCoreBundle.message("debug.error.script.read.failed", e.message.orEmpty()))
  }
  catch (e: UnexpectedScriptContentException) {
    throw ExecutionException(BazelCLionCoreBundle.message("debug.error.script.parse.failed", e.message.orEmpty()))
  }
  catch (e: RunfileManifestOnlyException) {
    throw ExecutionException(BazelCLionCoreBundle.message("debug.error.script.parse.failed", e.message.orEmpty()))
  }
  finally {
    withContext(Dispatchers.IO) {
      // best effort cleanup
      runCatching { Files.delete(scriptPath) }
    }
  }
}
