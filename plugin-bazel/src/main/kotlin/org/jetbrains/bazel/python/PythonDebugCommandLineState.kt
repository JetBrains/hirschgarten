package org.jetbrains.bazel.python

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.rd.util.toPromise
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
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bazel.commons.label.label
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.concurrency.await
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.findNameProvider
import org.jetbrains.plugins.bsp.run.BspCommandLineStateBase
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.jetbrains.plugins.bsp.run.commandLine.transformProgramArguments
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericRunState
import org.jetbrains.plugins.bsp.run.task.BspRunTaskListener
import org.jetbrains.plugins.bsp.target.TargetUtils
import org.jetbrains.plugins.bsp.target.targetUtils
import org.jetbrains.plugins.bsp.taskEvents.BspTaskListener
import org.jetbrains.plugins.bsp.taskEvents.OriginId

class PythonDebugCommandLineState(
  env: ExecutionEnvironment,
  originId: OriginId,
  private val settings: GenericRunState,
) : BspCommandLineStateBase(env, originId) {
  val targetId: BuildTargetIdentifier? = (env.runProfile as? BspRunConfiguration)?.targets?.singleOrNull() // temporary, for BSP cooperation
  val target: Label? = targetId?.label()
  private val scriptName = target?.let { PythonDebugUtils.guessRunScriptName(env.project, it) }

  override fun createAndAddTaskListener(handler: BspProcessHandler): BspTaskListener = BspRunTaskListener(handler)

  override suspend fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities) {
    val configuration = environment.runProfile as BspRunConfiguration
    val targetId = configuration.targets.single()
    val buildParams = CompileParams(listOf(targetId))
    buildParams.originId = originId
    buildParams.arguments = transformProgramArguments(settings.programArguments)

    server.buildTargetCompile(buildParams).toPromise().await()
  }

  fun asPythonState(): PythonCommandLineState = PythonScriptCommandLineState(pythonConfig(), environment)

  private fun pythonConfig(): PythonRunConfiguration =
    if (targetId == null || target == null) {
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
        it.sdk = getSdkForTarget(environment.project, targetId)
      }
    }
}

private fun getSdkForTarget(project: Project, target: BuildTargetIdentifier): Sdk {
  val storage = WorkspaceModel.getInstance(project).currentSnapshot
  val targetUtils = project.targetUtils
  val moduleNameProvider = project.findNameProvider()
  return moduleNameProvider
    ?.let { target.toModuleEntity(storage, it, targetUtils) } // module
    ?.dependencies // module's dependencies
    ?.firstNotNullOfOrNull { it as? SdkDependency } // first SDK dependency
    ?.sdk
    ?.name
    ?.let { PythonSdkUtil.getAllSdks().firstOrNull { sdk -> sdk.name == it } } // the first SDK matching the module SDK dependency
    ?: error(BazelPluginBundle.message("python.debug.error.no.sdk", target))
}

private fun BuildTargetIdentifier.toModuleEntity(
  storage: ImmutableEntityStorage,
  moduleNameProvider: TargetNameReformatProvider,
  targetUtils: TargetUtils,
): ModuleEntity? {
  val targetInfo = targetUtils.getBuildTargetInfoForId(this) ?: return null
  val moduleName = moduleNameProvider(targetInfo)
  val moduleId = ModuleId(moduleName)
  return storage.resolve(moduleId)
}
