package org.jetbrains.bazel.golang.debug

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.golang.targetKinds.includesGo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasBazelParams
import org.jetbrains.bazel.server.tasks.ScriptPathBuildTargetTask
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier
import kotlin.io.path.readText


private const val PROVIDER_NAME = "ScriptPathDebugBeforeRunTaskProvider"

private val PROVIDER_ID = Key.create<ScriptPathDebugBeforeRunTaskProvider.Task>(PROVIDER_NAME)

private val LOG = logger<ScriptPathDebugBeforeRunTaskProvider>()

internal class ScriptPathDebugBeforeRunTaskProvider : BeforeRunTaskProvider<ScriptPathDebugBeforeRunTaskProvider.Task>() {
  class Task : BeforeRunTask<Task>(PROVIDER_ID)

  override fun createTask(runConfiguration: RunConfiguration): Task? {
    if (!BazelFeatureFlags.isGoSupportEnabled) return null
    if (runConfiguration !is BazelRunConfiguration) return null
    return Task()
  }

  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName() = PROVIDER_NAME

  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: Task,
  ): Boolean {
    val runConfiguration = BazelRunConfiguration.get(environment)
    // EXECUTABLE_KEY is not present for non-debugging run config
    val executableKey = environment.getCopyableUserData(EXECUTABLE_KEY) ?: return true
    val project = environment.project
    val targetUtils = project.targetUtils
    val targetInfos = runConfiguration.targets.mapNotNull { targetUtils.getBuildTargetForLabel(it) }
    if (targetInfos.any { !it.kind.includesGo() || (it.kind.ruleType != RuleType.TEST && it.kind.ruleType != RuleType.BINARY) }) {
      return false
    }
    val target = runConfiguration.targets.single()
    val scriptPath = ScriptPathBuildTargetTask.createTempScriptFile()
    val extraBazelParams = listOf("--dynamic_mode=off", "--compilation_mode=dbg") + runConfiguration.extractAdditionalBazelParams()
    // runBlocking instead of runBlockingCancellable because before run tasks aren't cancellable
    val status = runBlocking {
      runBuildTargetTask(
        targetIds = listOf(target),
        project = project,
        isDebug = true,
        buildTargetTask = ScriptPathBuildTargetTask(
          scriptPath = scriptPath,
          programArguments = emptyList(),
          additionalBazelParams = extraBazelParams,
        ),
      )
    }
    if (status != BazelStatus.SUCCESS) return false
    val scriptContent = runCatching { scriptPath.readText() }
      .onFailure { handleScriptReadingErrors(it, target) }
      .getOrNull() ?: return false
    val executableInfo = runCatching { ExecutableInfo.fromBazelRunScript(scriptContent) }
      .onFailure { handleScriptParsingErrors(it, target) }
      .getOrNull() ?: return false
    executableKey.set(executableInfo)
    return true
  }

  private fun BazelRunConfiguration.extractAdditionalBazelParams() = (handler?.state as? HasBazelParams)
    ?.additionalBazelParams
    ?.let(::transformProgramArguments)
    .orEmpty()

  private fun handleScriptParsingErrors(error: Throwable, target: Label) {
    val content = when (error) {
      is UnexpectedScriptContentException -> BazelPluginBundle.message("go.debug.prepare.failed.reason.unexpected.script.content")
      is RunfileManifestOnlyException -> BazelPluginBundle.message("go.debug.prepare.failed.reason.runfile.manifest.only")
      else -> BazelPluginBundle.message("go.debug.prepare.failed.reason.unexpected.error")
    }
    LOG.error("Unable to prepare Go binary for debugging. Failed while parsing the script.", error)
    showScriptError(target, content)
  }

  private fun handleScriptReadingErrors(error: Throwable, target: Label) {
    LOG.error("Unable to prepare Go binary for debugging. Failed to read the script.", error)
    showScriptError(target, BazelPluginBundle.message("go.debug.prepare.failed.reason.read.script"))
  }

  private fun showScriptError(target: Label, content: String) {
    val title = BazelPluginBundle.message("go.debug.prepare.failed.title", target.toString())
    BazelBalloonNotifier.error(title, content)
  }
}
