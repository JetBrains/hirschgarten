package org.jetbrains.plugins.bsp.runnerAction

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.bsp.buildTask.BspProjectModuleBuildTasksTracker
import org.jetbrains.plugins.bsp.building.BspConsoleService
import org.jetbrains.plugins.bsp.building.TaskConsole
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.util.getModule
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import javax.swing.Icon
import kotlin.coroutines.cancellation.CancellationException

public abstract class LocalJvmRunnerAction(
  protected val targetInfo: BuildTargetInfo,
  text: () -> String,
  icon: Icon? = null,
  private val isDebugMode: Boolean = false,
) : BaseRunnerAction(targetInfo, text, icon, isDebugMode) {
  public abstract suspend fun getEnvironment(project: Project): JvmEnvironmentItem?

  override suspend fun getRunnerSettings(project: Project, buildTargetInfo: BuildTargetInfo): RunnerAndConfigurationSettings? {
    val module = targetInfo.getModule(project) ?: return null

    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    val environment = queryJvmEnvironment(project, bspSyncConsole) ?: return null
    return calculateConfigurationSettings(environment, module, project, targetInfo)
  }

  private fun calculateConfigurationSettings(
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
    targetInfo: BuildTargetInfo,
  ): RunnerAndConfigurationSettings? {
    val mainClass =
      environment.mainClasses?.firstOrNull() ?: return null // TODO https://youtrack.jetbrains.com/issue/BAZEL-626
    val applicationConfiguration =
      ApplicationConfiguration(
        calculateConfigurationName(targetInfo),
        project,
      ).apply {
        setModule(module)
        mainClassName = mainClass.className
        programParameters = mainClass.arguments.joinToString(" ")
        putUserData(jvmEnvironment, environment)
        putUserData(targetsToPreBuild, listOf(targetInfo.id))
        putUserData(includeJpsClassPaths, BspProjectModuleBuildTasksTracker.getInstance(project).lastBuiltByJps)
        beforeRunTasks = createBeforeRunBuildTask(this)
        shortenCommandLine = ShortenCommandLine.MANIFEST
      }
    val runManager = RunManagerImpl.getInstanceImpl(project)
    return RunnerAndConfigurationSettingsImpl(runManager, applicationConfiguration)
  }

  private fun createBeforeRunBuildTask(applicationConfiguration: ApplicationConfiguration) =
    BuildBeforeLocalRunTaskProvider()
      .createTask(applicationConfiguration)
      ?.let { listOf(it) } ?: emptyList()

  private fun createBspConfiguration(project: Project, targetInfo: BuildTargetInfo) =
    BspRunConfiguration(project, targetInfo.buildTargetName).apply { updateTargets(listOf(targetInfo.id)) }

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

  public companion object {
    public val jvmEnvironment: Key<JvmEnvironmentItem> = Key<JvmEnvironmentItem>("jvmEnvironment")
    public val targetsToPreBuild: Key<List<BuildTargetIdentifier>> = Key<List<BuildTargetIdentifier>>("jvmEnvironment")
    public val includeJpsClassPaths: Key<Boolean> = Key<Boolean>("includeJpsClassPaths")
  }
}

private const val RETRIEVE_JVM_ENVIRONMENT_ID = "bsp-retrieve-jvm-environment"
