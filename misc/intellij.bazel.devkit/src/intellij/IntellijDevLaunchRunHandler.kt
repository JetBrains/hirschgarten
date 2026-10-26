package org.jetbrains.bazel.intellij

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.util.EnvironmentUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.apparentRepoNameToCanonicalName
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.jvm.run.BazelJvmDebugRunner
import org.jetbrains.bazel.jvm.run.COROUTINE_JVM_FLAGS_KEY
import org.jetbrains.bazel.jvm.run.JvmDebuggableCommandLineState
import org.jetbrains.bazel.jvm.run.JvmRunState
import org.jetbrains.bazel.jvm.run.KotlinCoroutineLibraryFinderBeforeRunTaskProvider
import org.jetbrains.bazel.jvm.run.SCRIPT_PATH_KEY
import org.jetbrains.bazel.jvm.run.ScriptPathBeforeRunTaskProvider
import org.jetbrains.bazel.jvm.run.attachJvmRunExtensions
import org.jetbrains.bazel.jvm.run.getJvmDebuggerAgentVmOptions
import org.jetbrains.bazel.jvm.run.getJvmRunExtensionVmOptions
import org.jetbrains.bazel.jvm.run.runJvmCommandLine
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.repoMapping
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.commandLine.parseAsProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.snapshot.workspaceSnapshotOrNull
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.BuildTarget
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Runs and debugs an `intellij_dev_java_launcher` target.
 * The handler builds the target with `bazel run --script_path`, which also makes the runfiles tree.
 * Then it starts the native launcher of the target from its launch file, as `bazel run` starts its stub.
 * The debug options and the options of the run configuration extensions reach the JVM through a copy of the vmoptions file
 * of the target, see [writeIntellijDevVmOptionsCopy].
 * A program argument of the run configuration comes after the ones of the target.
 */
internal class IntellijDevLaunchRunHandler(private val configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    configuration.setBeforeRunTasksFromHandler(
      listOfNotNull(
        KotlinCoroutineLibraryFinderBeforeRunTaskProvider().createTask(configuration),
        ScriptPathBeforeRunTaskProvider().createTask(configuration),
      ),
    )
  }

  override val name: String
    get() = BazelDevKitBundle.message("dev.launch.run.handler.name")

  override val isTestHandler: Boolean = false

  override val state: JvmRunState = JvmRunState(configuration.project)

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    if (executor is DefaultDebugExecutor) {
      environment.putCopyableUserData(COROUTINE_JVM_FLAGS_KEY, Ref())
    }
    // The script path makes the build task run `bazel run --script_path`, which links the runfiles tree.
    environment.putCopyableUserData(SCRIPT_PATH_KEY, Ref())
    return IntellijDevLaunchCommandLineState(environment, state, configuration)
  }

  override val extensionsManager: RunConfigurationExtensionsManager<in RunConfigurationBase<*>, *>
    get() = JavaRunConfigurationExtensionManager.instance
}

internal class IntellijDevLaunchRunHandlerProvider : RunHandlerProvider {
  override val id: String
    get() = "IntellijDevLaunchRunHandlerProvider"

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = IntellijDevLaunchRunHandler(configuration)

  override fun canRun(project: Project, targets: List<BuildTarget>): Boolean =
    targets.singleOrNull()?.kind?.kind == INTELLIJ_DEV_JAVA_LAUNCHER_KIND
}

/** Attaches the debugger to an `intellij_dev_java_launcher` target, as [BazelJvmDebugRunner] does for a JVM binary. */
internal class IntellijDevLaunchDebugRunner : BazelJvmDebugRunner() {
  override fun getRunnerId(): String = "IntellijDevLaunchDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean =
    executorId == DefaultDebugExecutor.EXECUTOR_ID &&
    profile is BazelRunConfiguration &&
    profile.targets.size == 1 &&
    profile.handler is IntellijDevLaunchRunHandler
}

private class IntellijDevLaunchCommandLineState(
  environment: ExecutionEnvironment,
  private val settings: JvmRunState,
  private val configuration: BazelRunConfiguration,
) : JvmDebuggableCommandLineState(environment, settings.debugPort, configuration) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelRunTaskListener(handler)

  override suspend fun startBsp(server: BazelServerFacade, pidDeferred: CompletableDeferred<Long?>, handler: BazelProcessHandler) {
    val project = environment.project
    val target = configuration.targets.single()
    val launchFile = findLaunchFile(project, target)
    val launch = withContext(Dispatchers.IO) { readLaunch(launchFile, target) }

    val vmOptions = buildList {
      if (environment.executor is DefaultDebugExecutor) {
        add(jdwpVmOption(settings.debugPort))
      }
      addAll(getJvmDebuggerAgentVmOptions(environment))
      addAll(getJvmRunExtensionVmOptions(environment))
    }
    val vmOptionsCopy = withContext(Dispatchers.IO) {
      try {
        ensureIntellijDevLauncherFile(launch)
        if (vmOptions.isEmpty()) null else writeIntellijDevVmOptionsCopy(launch, vmOptions, vmOptionsCopyDirectory())
      }
      catch (e: IOException) {
        throw ExecutionException(BazelDevKitBundle.message("dev.launch.error.start", target, e.message ?: e.toString()), e)
      }
      catch (e: IllegalArgumentException) {
        throw ExecutionException(BazelDevKitBundle.message("dev.launch.error.start", target, e.message ?: e.toString()), e)
      }
    }
    val workspaceRoot = project.rootDir.toNioPath()
    val commandLine =
      GeneralCommandLine(intellijDevLaunchCommand(launch, parseAsProgramArguments(settings.programArguments)))
        .withWorkingDirectory(Path.of(launch.workingDirectory))
        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.NONE)
        .withEnvironment(
          intellijDevLaunchEnvironment(
            launch = launch,
            parentEnvironment = EnvironmentUtil.getEnvironmentMap(),
            runConfigurationEnvironment = settings.env.envs,
            workspaceRoot = workspaceRoot.toString(),
            vmOptionsCopy = vmOptionsCopy,
          ),
        )
    // The native launcher runs the JVM in its own process, so the pid of the launcher is the pid of the JVM.
    runJvmCommandLine(commandLine, pidDeferred, handler) { processHandler ->
      attachJvmRunExtensions(environment, processHandler)
    }
  }
}

/**
 * The directory of the vmoptions copies, in the system directory of the IDE that starts the target.
 * Monorepo DevKit writes its copies into another directory, so the two plugins never replace a copy of the other.
 */
private fun vmOptionsCopyDirectory(): Path = PathManager.getSystemDir().resolve("bazel-dev-launch")

/** The launch file of [target] in `bazel-bin`, in the main repository or in an external repository. */
private fun findLaunchFile(project: Project, target: Label): Path {
  // TODO: this may be the wrong bazel bin path if the configuration adds Bazel flags that change the output just for this target.
  // Since we're only using this for our monorepo, and the monorepo doesn't do such things,
  // adding code to our BEP support just for this one case isn't worth the time and effort for now.
  val bazelInfo = project.workspaceSnapshotOrNull?.bazelInfo?.takeUnless { it === BazelInfo.DEFAULT }
  val bazelBinPath = bazelInfo?.bazelBin ?: project.rootDir.toNioPath().resolve("out/bazel-bin")
  return bazelBinPath?.let { intellijDevLaunchFile(it, target, project.repoMapping.apparentRepoNameToCanonicalName) }
         ?: throw ExecutionException(BazelDevKitBundle.message("dev.launch.error.no.bazel.bin", target))
}

// The handler starts a local launcher process, so the launch file is a local file too.
@Suppress("UseOptimizedEelFunctions")
private fun readLaunch(launchFile: Path, target: Label): IntellijDevLaunch {
  val text = try {
    Files.readString(launchFile)
  }
  catch (_: IOException) {
    throw ExecutionException(BazelDevKitBundle.message("dev.launch.error.no.launch.file", launchFile, target))
  }
  try {
    return IntellijDevLaunch.parse(text)
  }
  catch (e: RuntimeException) {
    throw ExecutionException(BazelDevKitBundle.message("dev.launch.error.invalid.launch.file", launchFile, e.message ?: e.toString()), e)
  }
}
