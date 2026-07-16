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
import com.jetbrains.python.sdk.ModuleOrProject.ProjectOnly
import com.jetbrains.python.sdk.createLocalSdkGuessingTypeByPath
import com.jetbrains.python.sdk.legacy.PythonSdkUtil
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.debugFlags
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.CompileParams
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal class PythonDebugCommandLineState(
  private val environment: ExecutionEnvironment,
  private val programArguments: String?,
  val additionalBazelParams: String?,
) :
  BazelCommandLineStateBase(environment) {
  val target: Label? = (environment.runProfile as? BazelRunConfiguration)?.targets?.singleOrNull()

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
        arguments = buildPythonDebugBazelArguments(server.projectView.debugFlags, additionalBazelParams),
      )
    server.buildTargetCompile(buildParams)
  }

  suspend fun asPythonState(): PythonCommandLineState = PythonScriptCommandLineState(pythonConfig(), environment)

  private suspend fun pythonConfig(): PythonRunConfiguration {
    val debugInfo = target?.let {
      PythonDebugUtils.preparePythonDebug(environment.project, it)
    }
    if (target == null) {
      error(BazelPluginBundle.message("python.debug.error.no.id"))
    } else if (debugInfo == null) {
      error(BazelPluginBundle.message("python.debug.error.other", target))
    }
    val templateConfig =
      PythonConfigurationType
        .getInstance()
        .factory
        .createTemplateConfiguration(environment.project)
        as PythonRunConfiguration // should always succeed; that's what PythonConfigurationFactory produces
    return templateConfig.also {
      it.scriptName = debugInfo.pythonFile.toAbsolutePath().toString()
      it.scriptParameters = programArguments
      it.workingDirectory = debugInfo.workingDirectory.toAbsolutePath().toString()
      it.envs = debugInfo.environmentVariables
      it.sdk =
        debugInfo.pythonBinary?.let { pythonBinary -> getOrCreateSdkForPythonBinary(environment.project, pythonBinary) }
        ?: getSdkForTarget(environment.project, target)
    }
  }
}

internal suspend fun getOrCreateSdkForPythonBinary(project: Project, pythonBinary: Path): Sdk? {
  if (!pythonBinary.isAbsolute || !Files.isRegularFile(pythonBinary)) return null
  getSdkForPythonBinary(pythonBinary)?.let { return it }
  return when (val result = createLocalSdkGuessingTypeByPath(pythonBinary, ProjectOnly(project))) {
    is com.jetbrains.python.Result.Failure -> null
    is com.jetbrains.python.Result.Success -> result.result
  }
}

internal fun getSdkForPythonBinary(pythonBinary: Path): Sdk? {
  if (!pythonBinary.isAbsolute) return null
  val normalizedPythonBinary = pythonBinary.toSdkLookupPath()
  return PythonSdkUtil.getAllSdks().firstOrNull { sdk ->
    sdk.homePath?.let { sdkHome ->
      runCatching { Path.of(sdkHome).toSdkLookupPath() == normalizedPythonBinary }.getOrDefault(false)
    } == true
  }
}

private fun Path.toSdkLookupPath(): Path =
  runCatching { toRealPath(LinkOption.NOFOLLOW_LINKS) }.getOrElse { toAbsolutePath().normalize() }

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

internal fun buildPythonDebugBazelArguments(debugFlags: List<String>, additionalBazelParams: String?): List<String> =
  debugFlags + transformProgramArguments(additionalBazelParams)
