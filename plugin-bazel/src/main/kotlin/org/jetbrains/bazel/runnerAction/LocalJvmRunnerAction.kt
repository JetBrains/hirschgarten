package org.jetbrains.bazel.runnerAction

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.projectAware.BspProjectModuleBuildTasksTracker
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bazel.target.getModule
import org.jetbrains.bazel.ui.console.BspConsoleService
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.StatusCode
import javax.swing.Icon
import kotlin.coroutines.cancellation.CancellationException

abstract class LocalJvmRunnerAction(
  protected val targetInfo: BuildTargetInfo,
  text: () -> String,
  icon: Icon? = null,
  private val isDebugMode: Boolean = false,
) : BaseRunnerAction(listOf(targetInfo), text, icon, isDebugMode) {
  abstract suspend fun getEnvironment(project: Project): JvmEnvironmentItem?

  override suspend fun getRunnerSettings(project: Project, buildTargetInfos: List<BuildTargetInfo>): RunnerAndConfigurationSettings? {
    val module = targetInfo.getModule(project) ?: return null

    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    if (!preBuild(project)) return null
    val environment = queryJvmEnvironment(project, bspSyncConsole) ?: return null
    return calculateConfigurationSettings(environment, module, project, targetInfo)
  }

  private suspend fun preBuild(project: Project): Boolean {
    val buildResult = runBuildTargetTask(listOf(targetInfo.id), project)
    return buildResult?.statusCode == StatusCode.OK
  }

  private fun calculateConfigurationSettings(
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    targetInfo: BuildTargetInfo,
  ): RunnerAndConfigurationSettings? {
    val mainClass =
      environment.mainClasses?.firstOrNull() ?: return null // TODO https://youtrack.jetbrains.com/issue/BAZEL-626
    val configuration =
      BspJvmApplicationConfiguration(
        calculateConfigurationName(targetInfo),
        project,
      ).apply {
        setModule(module)
        mainClassName = mainClass.className
        programParameters = mainClass.arguments.joinToString(" ")
        putUserData(jvmEnvironment, environment)
        putUserData(targetsToPreBuild, listOf(targetInfo.id))
        putUserData(includeJpsClassPaths, BspProjectModuleBuildTasksTracker.getInstance(project).lastBuiltByJps)
        shortenCommandLine = ShortenCommandLine.MANIFEST
      }
    val runManager = RunManagerImpl.getInstanceImpl(project)
    return RunnerAndConfigurationSettingsImpl(runManager, configuration)
  }

  private fun calculateConfigurationName(targetInfo: BuildTargetInfo): String {
    val targetDisplayName = targetInfo.buildTargetName
    val actionNameKey =
      when {
        isDebugMode -> "target.debug.with.jvm.runner.config.name"
        this is TestWithLocalJvmRunnerAction -> "target.test.with.jvm.runner.config.name"
        else -> "target.run.with.jvm.runner.config.name"
      }
    return BspPluginBundle.message(actionNameKey, targetDisplayName)
  }

  private suspend fun queryJvmEnvironment(project: Project, bspSyncConsole: TaskConsole) =
    try {
      withContext(Dispatchers.IO) {
        val job = async { getEnvironment(project) }
        bspSyncConsole.startTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BspPluginBundle.message("console.task.query.jvm.environment.title"),
          BspPluginBundle.message("console.task.query.jvm.environment.in.progress"),
          { job.cancel() },
        )
        val env = job.await()
        bspSyncConsole.finishTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BspPluginBundle.message("console.task.query.jvm.environment.success"),
        )
        env
      }
    } catch (e: Exception) {
      if (e is CancellationException) {
        bspSyncConsole.finishTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BspPluginBundle.message("console.task.query.jvm.environment.cancel"),
          FailureResultImpl(),
        )
      } else {
        bspSyncConsole.finishTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BspPluginBundle.message("console.task.query.jvm.environment.failed"),
          FailureResultImpl(e),
        )
      }
      null
    }

  companion object {
    val jvmEnvironment: Key<JvmEnvironmentItem> = Key<JvmEnvironmentItem>("jvmEnvironment")
    val targetsToPreBuild: Key<List<BuildTargetIdentifier>> = Key<List<BuildTargetIdentifier>>("targetsToPreBuild")
    val includeJpsClassPaths: Key<Boolean> = Key<Boolean>("includeJpsClassPaths")
  }
}

private val log = logger<LocalJvmRunnerAction>()

private const val RETRIEVE_JVM_ENVIRONMENT_ID = "bsp-retrieve-jvm-environment"

class BspJvmApplicationConfiguration(name: String, project: Project) : ApplicationConfiguration(name, project)
