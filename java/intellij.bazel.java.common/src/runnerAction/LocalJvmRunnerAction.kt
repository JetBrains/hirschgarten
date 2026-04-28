package org.jetbrains.bazel.runnerAction

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.execution.Executor
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.projectAware.BazelProjectModuleBuildTasksTracker
import org.jetbrains.bazel.run.config.HotswappableRunConfiguration
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bazel.target.getModule
import org.jetbrains.bsp.protocol.ExecutableTarget
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.TaskGroupId
import kotlin.coroutines.cancellation.CancellationException

@ApiStatus.Internal
abstract class LocalJvmRunnerAction(
  private val project: Project,
  protected val target: ExecutableTarget,
  configurationName: String,
  private val executor: Executor,
) : BaseRunnerAction(executor, configurationName) {

  abstract suspend fun getEnvironment(project: Project): JvmEnvironmentItem?

  override suspend fun getRunnerSettings(): RunnerAndConfigurationSettings? {
    val module = target.id.getModule(project) ?: return null

    if (!preBuild(project)) return null
    val environment = queryJvmEnvironment(project) ?: return null
    return calculateConfigurationSettings(environment, module, project, target)
  }

  private suspend fun preBuild(project: Project): Boolean {
    val statusCode = runBuildTargetTask(listOf(target.id), project, isDebug = executor.id == DefaultDebugExecutor.EXECUTOR_ID)
    return statusCode == BazelStatus.SUCCESS
  }

  private fun calculateConfigurationSettings(
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    targetInfo: ExecutableTarget,
  ): RunnerAndConfigurationSettings? {
    val configuration =
      calculateConfiguration(this@LocalJvmRunnerAction.configurationName, environment, module, project, targetInfo) ?: return null
    val runManager = RunManagerImpl.getInstanceImpl(project)
    return RunnerAndConfigurationSettingsImpl(runManager, configuration)
  }

  protected open fun calculateConfiguration(
    configurationName: String,
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    targetInfo: ExecutableTarget,
  ): RunConfiguration? {
    val mainClass =
      environment.mainClasses?.firstOrNull() ?: return null // TODO https://youtrack.jetbrains.com/issue/BAZEL-626
    val configuration =
      BspJvmApplicationConfiguration(configurationName, project).apply {
        setModule(module)
        mainClassName = mainClass.className
        programParameters = mainClass.arguments.joinToString(" ")
        putUserData(jvmEnvironment, environment)
        putUserData(targetsToPreBuild, listOf(targetInfo.id))
        putUserData(includeJpsClassPaths, BazelProjectModuleBuildTasksTracker.getInstance(project).lastBuiltByJps)
        shortenCommandLine = ShortenCommandLine.MANIFEST
      }
    return configuration
  }

  private suspend fun queryJvmEnvironment(project: Project): JvmEnvironmentItem? {
    val bspSyncConsole = ConsoleService.getInstance(project).syncConsole
    val taskId = TaskGroupId("LocalJvmRunnerAction").task(RETRIEVE_JVM_ENVIRONMENT_ID)
    return try {
      withContext(Dispatchers.IO) {
        val job = async { getEnvironment(project) }
        bspSyncConsole.startTask(
          taskId,
          BazelPluginBundle.message("console.task.query.jvm.environment.title"),
          BazelPluginBundle.message("console.task.query.jvm.environment.in.progress"),
          { job.cancel() },
        )
        val env = job.await()
        bspSyncConsole.finishTask(
          taskId,
          BazelPluginBundle.message("console.task.query.jvm.environment.success"),
        )
        env
      }
    }
    catch (e: Exception) {
      if (e is CancellationException) {
        bspSyncConsole.finishTask(
          taskId,
          BazelPluginBundle.message("console.task.query.jvm.environment.cancel"),
          FailureResultImpl(),
        )
      }
      else {
        bspSyncConsole.finishTask(
          taskId,
          BazelPluginBundle.message("console.task.query.jvm.environment.failed"),
          FailureResultImpl(e),
        )
      }
      null
    }
  }

  companion object {
    val jvmEnvironment: Key<JvmEnvironmentItem> = Key<JvmEnvironmentItem>("jvmEnvironment")
    val targetsToPreBuild: Key<List<Label>> = Key<List<Label>>("targetsToPreBuild")
    val includeJpsClassPaths: Key<Boolean> = Key<Boolean>("includeJpsClassPaths")
  }
}

private const val RETRIEVE_JVM_ENVIRONMENT_ID = "bsp-retrieve-jvm-environment"

internal class BspJvmApplicationConfiguration(name: String, project: Project) :
  ApplicationConfiguration(name, project),
  HotswappableRunConfiguration {
  override fun getAffectedTargets(): List<Label> = getUserData(LocalJvmRunnerAction.targetsToPreBuild)?.take(1) ?: listOf()
}
