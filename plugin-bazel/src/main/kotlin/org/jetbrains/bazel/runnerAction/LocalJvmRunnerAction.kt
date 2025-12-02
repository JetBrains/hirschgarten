package org.jetbrains.bazel.runnerAction

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.projectAware.BazelProjectModuleBuildTasksTracker
import org.jetbrains.bazel.run.config.HotswappableRunConfiguration
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bazel.target.getModule
import org.jetbrains.bazel.ui.console.ConsoleService
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import javax.swing.Icon
import kotlin.coroutines.cancellation.CancellationException

abstract class LocalJvmRunnerAction(
  protected val targetInfo: BuildTarget,
  text: () -> String,
  icon: Icon? = null,
  private val isDebugMode: Boolean = false,
) : BaseRunnerAction(listOf(targetInfo), text, icon, isDebugMode) {
  abstract suspend fun getEnvironment(project: Project): JvmEnvironmentItem?

  override suspend fun getRunnerSettings(project: Project, buildTargets: List<BuildTarget>): RunnerAndConfigurationSettings? {
    val module = targetInfo.getModule(project) ?: return null

    val bspSyncConsole = ConsoleService.getInstance(project).syncConsole
    if (!preBuild(project)) return null
    val environment = queryJvmEnvironment(project, bspSyncConsole) ?: return null
    return calculateConfigurationSettings(environment, module, project, targetInfo)
  }

  private suspend fun preBuild(project: Project): Boolean {
    val statusCode = runBuildTargetTask(listOf(targetInfo.id), project, isDebugMode)
    return statusCode == BazelStatus.SUCCESS
  }

  private fun calculateConfigurationSettings(
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    targetInfo: BuildTarget,
  ): RunnerAndConfigurationSettings? {
    val configurationName = calculateConfigurationName(project, targetInfo)
    val configuration = calculateConfiguration(configurationName, environment, module, project, targetInfo) ?: return null
    val runManager = RunManagerImpl.getInstanceImpl(project)
    return RunnerAndConfigurationSettingsImpl(runManager, configuration)
  }

  protected open fun calculateConfiguration(
    configurationName: String,
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    targetInfo: BuildTarget,
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

  private fun calculateConfigurationName(project: Project, targetInfo: BuildTarget): String {
    val targetDisplayName = targetInfo.id.toShortString(project)
    val actionNameKey =
      when {
        isDebugMode -> "target.debug.with.jvm.runner.config.name"
        this is TestWithLocalJvmRunnerAction -> "target.test.with.jvm.runner.config.name"
        else -> "target.run.with.jvm.runner.config.name"
      }
    return BazelPluginBundle.message(actionNameKey, targetDisplayName)
  }

  private suspend fun queryJvmEnvironment(project: Project, bspSyncConsole: TaskConsole) =
    try {
      withContext(Dispatchers.IO) {
        val job = async { getEnvironment(project) }
        bspSyncConsole.startTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BazelPluginBundle.message("console.task.query.jvm.environment.title"),
          BazelPluginBundle.message("console.task.query.jvm.environment.in.progress"),
          { job.cancel() },
        )
        val env = job.await()
        bspSyncConsole.finishTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BazelPluginBundle.message("console.task.query.jvm.environment.success"),
        )
        env
      }
    } catch (e: Exception) {
      if (e is CancellationException) {
        bspSyncConsole.finishTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BazelPluginBundle.message("console.task.query.jvm.environment.cancel"),
          FailureResultImpl(),
        )
      } else {
        bspSyncConsole.finishTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BazelPluginBundle.message("console.task.query.jvm.environment.failed"),
          FailureResultImpl(e),
        )
      }
      null
    }

  companion object {
    val jvmEnvironment: Key<JvmEnvironmentItem> = Key<JvmEnvironmentItem>("jvmEnvironment")
    val targetsToPreBuild: Key<List<Label>> = Key<List<Label>>("targetsToPreBuild")
    val includeJpsClassPaths: Key<Boolean> = Key<Boolean>("includeJpsClassPaths")
  }
}

private const val RETRIEVE_JVM_ENVIRONMENT_ID = "bsp-retrieve-jvm-environment"

class BspJvmApplicationConfiguration(name: String, project: Project) :
  ApplicationConfiguration(name, project),
  HotswappableRunConfiguration {
  override fun getAffectedTargets(): List<Label> = getUserData(LocalJvmRunnerAction.targetsToPreBuild)?.take(1) ?: listOf()
}
