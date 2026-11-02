package org.jetbrains.bazel.clion.run.debug

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.aspect.lib.INTELLIJ_CC_DEBUG_ASPECT
import com.intellij.aspect.lib.OutputGroups
import com.intellij.aspect.lib.readTargetFromFile
import com.intellij.build.events.MessageEvent
import com.intellij.execution.ExecutionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.clion.BazelCLionCoreBundle
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.languages.projectview.debugFlags
import org.jetbrains.bazel.run.commandLine.parseAsProgramArguments
import org.jetbrains.bazel.server.bsp.managers.resolveAspectLabel
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.server.tasks.ScriptPathBuildTargetTask
import org.jetbrains.bazel.utils.ExecutableInfo
import org.jetbrains.bazel.utils.UnexpectedScriptContentException
import org.jetbrains.bsp.protocol.RunParams
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

internal class CcDebugBuildResult(
  val executable: ExecutableInfo,

  /** All targets that Bazel built for the executable, empty if the debug aspect did not run. */
  val targets: List<TargetIdeInfo>,
)

/** Returns the target that the `intellij_cc_debug_aspect` reported for the label. */
internal fun Collection<TargetIdeInfo>.findDebugTarget(label: Label): TargetIdeInfo? {
  // it's very unlikely that the main target exists under two configurations here
  return firstOrNull { it.label() == label }
}

/**
 * Builds the target and writes the run script with one `bazel run --script_path`. The same command
 * also runs the `intellij_cc_debug_aspect`, if the last sync deployed it.
 */
@Throws(ExecutionException::class)
context(ctx: CcDebugContext)
internal suspend fun buildExecutable(): CcDebugBuildResult {
  val scriptPath = try {
    withContext(Dispatchers.IO) {
      Files.createTempFile("bazel-cc-debug-", ".sh")
    }
  }
  catch (e: IOException) {
    throw ExecutionException(BazelCLionCoreBundle.message("debug.error.script.create.failed"), e)
  }

  try {
    val aspectDeployed = isCcDebugAspectDeployed()
    if (!aspectDeployed) {
      ctx.reportEvent(MessageEvent.Kind.WARNING, BazelCLionCoreBundle.message("debug.warning.aspect.not.deployed"))
    }

    val (status, infoFiles) = ctx.project.connection.runWithServer(ctx.taskId) { server ->
      val bazelParams = buildList {
        addAll(ScriptPathBuildTargetTask.scriptPathParams(scriptPath))
        if (aspectDeployed) addAll(ccDebugAspectParams())
        addAll(server.projectView.debugFlags)
        addAll(parseAsProgramArguments(ctx.state.additionalBazelParams))
      }

      val params = RunParams(
        taskId = ctx.taskId,
        target = ctx.label,
        additionalBazelParams = bazelParams,
        checkVisibility = true,
        environmentVariables = ctx.state.env.envs,
        arguments = parseAsProgramArguments(ctx.state.programArguments),
      )

      saveAllFiles()
      val result = server.buildTargetRun(params)
      result.statusCode to result.infoFiles
    }

    if (status != BazelStatus.SUCCESS) {
      throw ExecutionException(BazelCLionCoreBundle.message("debug.error.script.generate.failed", status))
    }

    val executableInfo = withContext(Dispatchers.IO) {
      ExecutableInfo.fromBazelRunScript(Files.readString(scriptPath)).also { it.createRequiredDirectories() }
    }

    return CcDebugBuildResult(executableInfo, readDebugTargets(infoFiles))
  }
  catch (e: IOException) {
    throw ExecutionException(BazelCLionCoreBundle.message("debug.error.script.read.failed"), e)
  }
  catch (e: UnexpectedScriptContentException) {
    throw ExecutionException(BazelCLionCoreBundle.message("debug.error.script.parse.failed"), e)
  }
  finally {
    // best effort cleanup, also after a cancellation
    withContext(NonCancellable + Dispatchers.IO) {
      runCatching { Files.deleteIfExists(scriptPath) }
    }
  }
}

/** Reports all files that it cannot read as one warning. */
context(ctx: CcDebugContext)
private suspend fun readDebugTargets(infoFiles: Set<Path>): List<TargetIdeInfo> {
  val problems = mutableListOf<String>()
  val targets = withContext(Dispatchers.IO) {
    infoFiles.mapNotNull { path ->
      readTargetFromFile(path) { msg -> problems += "$path: ${msg.orEmpty()}" }
    }
  }

  if (problems.isNotEmpty()) {
    ctx.reportEvent(
      severity = MessageEvent.Kind.WARNING,
      message = BazelCLionCoreBundle.message("debug.warning.info.read.failed", problems.size),
      description = problems.joinToString("\n"),
    )
  }

  return targets
}

/**
 * Only a sync deploys the aspects, thus the files of an older plugin version can miss the debug aspect.
 *
 * TODO: this is a very simple heuristic, maybe could re-deploy the aspect here or check some sha-sum instead?
 */
context(ctx: CcDebugContext)
private suspend fun isCcDebugAspectDeployed(): Boolean {
  val path = ctx.project.rootDir.toNioPath()
    .resolve(Constants.DOT_BAZELBSP_DIR_NAME)
    .resolve(INTELLIJ_CC_DEBUG_ASPECT.location.file)

  return withContext(Dispatchers.IO) { Files.exists(path) }
}

private fun ccDebugAspectParams(): List<String> = listOf(
  BazelFlag.aspect(resolveAspectLabel(INTELLIJ_CC_DEBUG_ASPECT.location.toString())),
  // keep the default outputs, otherwise bazel does not build the executable
  BazelFlag.outputGroups(listOf("+${OutputGroups.INFO.groupName}", "+${OutputGroups.DEBUG.groupName}")),
)
