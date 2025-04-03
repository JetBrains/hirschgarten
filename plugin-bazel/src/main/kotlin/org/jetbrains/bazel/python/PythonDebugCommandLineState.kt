package org.jetbrains.bazel.python

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
import com.jetbrains.python.sdk.PythonSdkUtil
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.bazel.magicmetamodel.findNameProvider
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.sync.status.BuildStatusService
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.JoinedBuildServer

class PythonDebugCommandLineState(
  env: ExecutionEnvironment,
  originId: OriginId,
  private val settings: GenericRunState,
) : BazelCommandLineStateBase(env, originId) {
  val target: Label? = (env.runProfile as? BazelRunConfiguration)?.targets?.singleOrNull()
  private val scriptName = target?.let { PythonDebugUtils.guessRunScriptName(env.project, it) }

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelRunTaskListener(handler)

  override suspend fun startBsp(server: JoinedBuildServer) {
    val configuration = environment.runProfile as BazelRunConfiguration
    val targetId = configuration.targets.single()
    val buildParams =
      CompileParams(
        targets = listOf(targetId),
        originId = originId,
        arguments = transformProgramArguments(settings.programArguments),
      )
    BuildStatusService.getInstance(environment.project).withBuildInProgress {
      server.buildTargetCompile(buildParams)
    }
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
  val moduleNameProvider = project.findNameProvider()
  return moduleNameProvider
    .let { target.toModuleEntity(storage, it) } // module
    ?.dependencies // module's dependencies
    ?.firstNotNullOfOrNull { it as? SdkDependency } // first SDK dependency
    ?.sdk
    ?.name
    ?.let { PythonSdkUtil.getAllSdks().firstOrNull { sdk -> sdk.name == it } } // the first SDK matching the module SDK dependency
    ?: error(BazelPluginBundle.message("python.debug.error.no.sdk", target))
}

private fun Label.toModuleEntity(storage: ImmutableEntityStorage, moduleNameProvider: TargetNameReformatProvider): ModuleEntity? {
  val moduleName = moduleNameProvider(this)
  val moduleId = ModuleId(moduleName)
  return storage.resolve(moduleId)
}
