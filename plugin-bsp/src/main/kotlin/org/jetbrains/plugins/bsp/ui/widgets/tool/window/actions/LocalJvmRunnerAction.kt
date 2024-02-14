package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.util.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspProjectModuleBuildTasksTracker
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.utils.orDefault
import javax.swing.Icon
import kotlin.coroutines.cancellation.CancellationException

internal abstract class LocalJvmRunnerAction(
  protected val targetInfo: BuildTargetInfo,
  text: () -> String,
  icon: Icon? = null,
) : SuspendableAction(text, icon) {
  abstract fun getEnvironment(project: Project): JvmEnvironmentItem?

  abstract fun getExecutor(): Executor

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val moduleNameProvider = project.findModuleNameProvider().orDefault()
    val module = project.modules.find { it.name == moduleNameProvider(targetInfo) } ?: return

    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    queryJvmEnvironment(project, bspSyncConsole)?.let { runWithEnvironment(it, targetInfo.id, module, project) }
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

  protected open suspend fun runWithEnvironment(
    environment: JvmEnvironmentItem,
    uri: String,
    module: Module,
    project: Project,
  ) {
    environment.mainClasses
      ?.firstOrNull() // TODO https://youtrack.jetbrains.com/issue/BAZEL-626
      ?.let { mainClass ->
        val applicationConfiguration = ApplicationConfiguration(
          BspPluginBundle.message("console.task.run.with.jvm.env.config.name", uri), project
        ).apply {
          setModule(module)
          mainClassName = mainClass.className
          programParameters = mainClass.arguments.joinToString(" ")
          putUserData(jvmEnvironment, environment)
          putUserData(prioritizeIdeClasspath, BspProjectModuleBuildTasksTracker.getInstance(project).lastBuiltByJps)
        }
        val runManager = RunManagerImpl.getInstanceImpl(project)
        val settings = RunnerAndConfigurationSettingsImpl(runManager, applicationConfiguration)
        RunManager.getInstance(project).setTemporaryConfiguration(settings)
        val runExecutor = getExecutor()
        withContext(Dispatchers.EDT) {
          ProgramRunner.getRunner(runExecutor.id, settings.configuration)?.let { runner ->
            val executionEnvironment = ExecutionEnvironmentBuilder(project, runExecutor)
              .runnerAndSettings(runner, settings)
              .build()
            runner.execute(executionEnvironment)
          }
        }
      }
  }

  companion object {
    val jvmEnvironment = Key<JvmEnvironmentItem>("jvmEnvironment")
    val prioritizeIdeClasspath = Key<Boolean>("prioritizeIdeClasspath")
  }
}

private const val RETRIEVE_JVM_ENVIRONMENT_ID = "bsp-retrieve-jvm-environment"
