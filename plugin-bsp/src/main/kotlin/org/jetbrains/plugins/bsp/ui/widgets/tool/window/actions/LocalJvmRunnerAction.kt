package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.util.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspProjectModuleBuildTasksTracker
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.getBuildTargetName
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.utils.orDefault
import javax.swing.Icon
import kotlin.coroutines.cancellation.CancellationException

internal abstract class LocalJvmRunnerAction(
  protected val targetInfo: BuildTargetInfo,
  text: () -> String,
  icon: Icon? = null,
  private val isDebugMode: Boolean = false,
) : BaseRunnerAction(targetInfo, text, icon, isDebugMode) {
  abstract fun getEnvironment(project: Project): JvmEnvironmentItem?

  override suspend fun getRunnerSettings(
    project: Project,
    buildTargetInfo: BuildTargetInfo,
  ): RunnerAndConfigurationSettings? {
    val moduleNameProvider = project.findModuleNameProvider().orDefault()
    val module = project.modules.find { it.name == moduleNameProvider(targetInfo) } ?: return null

    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    val environment = queryJvmEnvironment(project, bspSyncConsole) ?: return null
    return calculateConfigurationSettings(environment, module, project)
  }

  private fun calculateConfigurationSettings(
    environment: JvmEnvironmentItem,
    module: Module,
    project: Project,
  ): RunnerAndConfigurationSettings? {
    val mainClass =
      environment.mainClasses?.firstOrNull() ?: return null // TODO https://youtrack.jetbrains.com/issue/BAZEL-626
    val applicationConfiguration = ApplicationConfiguration(
      calculateConfigurationName(targetInfo), project
    ).apply {
      setModule(module)
      mainClassName = mainClass.className
      programParameters = mainClass.arguments.joinToString(" ")
      putUserData(jvmEnvironment, environment)
      putUserData(prioritizeIdeClasspath, BspProjectModuleBuildTasksTracker.getInstance(project).lastBuiltByJps)
    }
    val runManager = RunManagerImpl.getInstanceImpl(project)
    return RunnerAndConfigurationSettingsImpl(runManager, applicationConfiguration)
  }

  private fun calculateConfigurationName(targetInfo: BuildTargetInfo): String {
    val targetDisplayName = targetInfo.getBuildTargetName()
    val actionNameKey = when {
      isDebugMode -> "target.debug.with.jvm.runner.config.name"
      this is TestWithLocalJvmRunnerAction -> "target.test.with.jvm.runner.config.name"
      else -> "target.run.with.jvm.runner.config.name"
    }
    return BspPluginBundle.message(actionNameKey, targetDisplayName)
  }

  private suspend fun queryJvmEnvironment(
    project: Project,
    bspSyncConsole: TaskConsole,
  ) =
    try {
      withContext(Dispatchers.IO) {
        val job = async { runInterruptible { getEnvironment(project) } }
        bspSyncConsole.startTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BspPluginBundle.message("console.task.query.jvm.environment.title"),
          BspPluginBundle.message("console.task.query.jvm.environment.in.progress"),
          { job.cancel() }
        )
        val env = job.await()
        bspSyncConsole.finishTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BspPluginBundle.message("console.task.query.jvm.environment.success")
        )
        env
      }
    } catch (e: Exception) {
      if (e is CancellationException)
        bspSyncConsole.finishTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BspPluginBundle.message("console.task.query.jvm.environment.cancel"),
          FailureResultImpl(BspPluginBundle.message("console.task.query.jvm.environment.cancel"))
        )
      else
        bspSyncConsole.finishTask(
          RETRIEVE_JVM_ENVIRONMENT_ID,
          BspPluginBundle.message("console.task.query.jvm.environment.failure"),
          FailureResultImpl(e)
        )
      null
    }

  companion object {
    val jvmEnvironment = Key<JvmEnvironmentItem>("jvmEnvironment")
    val prioritizeIdeClasspath = Key<Boolean>("prioritizeIdeClasspath")
  }
}

private const val RETRIEVE_JVM_ENVIRONMENT_ID = "bsp-retrieve-jvm-environment"
