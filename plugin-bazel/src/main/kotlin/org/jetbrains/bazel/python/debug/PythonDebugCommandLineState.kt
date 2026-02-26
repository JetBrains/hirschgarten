package org.jetbrains.bazel.python.debug

import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.jetbrains.python.run.PythonCommandLineState
import com.jetbrains.python.run.PythonConfigurationType
import com.jetbrains.python.run.PythonRunConfiguration
import com.jetbrains.python.run.PythonScriptCommandLineState
import com.jetbrains.python.sdk.legacy.PythonSdkUtil
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.BazelServerFacade

class PythonDebugCommandLineState(environment: ExecutionEnvironment, private val programArguments: String?) :
  BazelCommandLineStateBase(environment) {
  val target: Label? = (environment.runProfile as? BazelRunConfiguration)?.targets?.singleOrNull()
  private val scriptName = target?.let { PythonDebugUtils.guessRunScriptName(environment.project, it) }

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelRunTaskListener(handler)

  override suspend fun startBsp(
      server: BazelServerFacade,
      pidDeferred: CompletableDeferred<Long?>,
      handler: BazelProcessHandler,
  ) {
    val configuration = BazelRunConfiguration.get(environment)
    val targetId = configuration.targets.single()
    val buildParams =
      CompileParams(
        targets = listOf(targetId),
        taskId = taskGroupId.task("py-debug"),
        arguments = transformProgramArguments(programArguments),
      )
    server.buildTargetCompile(buildParams)
  }

  fun asPythonState(): PythonCommandLineState = PythonScriptCommandLineState(pythonConfig(), environment)

  private fun pythonConfig(): PythonRunConfiguration =
    if (target == null) {
      error(BazelPluginBundle.message("python.debug.error.no.id"))
    } else if (scriptName == null) {
      error(BazelPluginBundle.message("python.debug.error.no.script", target))
    } else {
      val templateConfig =
        PythonConfigurationType
          .getInstance()
          .factory
          .createTemplateConfiguration(environment.project)
          as PythonRunConfiguration // should always succeed; that's what PythonConfigurationFactory produces
      templateConfig.also {
        it.scriptName = scriptName.toAbsolutePath().toString()
        it.sdk = getSdkForTarget(environment.project, target)
      }
    }
}

private fun getSdkForTarget(project: Project, target: Label): Sdk {
  val storage = WorkspaceModel.getInstance(project).currentSnapshot
  return target
    .toModuleEntity(storage, project) // module
    ?.dependencies // module's dependencies
    ?.firstNotNullOfOrNull { it as? SdkDependency } // first SDK dependency
    ?.sdk
    ?.name
    ?.let { PythonSdkUtil.getAllSdks().firstOrNull { sdk -> sdk.name == it } } // the first SDK matching the module SDK dependency
         ?: error(BazelPluginBundle.message("python.debug.error.no.sdk", target))
}

private fun Label.toModuleEntity(storage: ImmutableEntityStorage, project: Project): ModuleEntity? {
  val moduleName = this.formatAsModuleName(project)
  val moduleId = ModuleId(moduleName)
  return storage.resolve(moduleId)
}
