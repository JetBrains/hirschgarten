package org.jetbrains.bazel.ui.widgets.queryTab

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.bazelrunner.BazelProcess
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class BazelFlag(value: String) {
  val value = withDoubleHyphen(value)

  companion object {
    fun withDoubleHyphen(flagToParse: String): String {
      val flag = flagToParse.trim()
      val initialHyphens = flag.indexOfFirst { it != '-' }
      return when {
        initialHyphens < 2 -> {
          "-".repeat(2 - initialHyphens) + flag
        }
        initialHyphens > 2 -> flag.substring(2 - initialHyphens)
        else -> flag
      }
    }

    fun fromTextField(textFieldContents: String): MutableList<BazelFlag> {
      val result = mutableListOf<BazelFlag>()
      if (textFieldContents.isNotEmpty()) {
        for (flag in textFieldContents.trim().split(" -")) {
          val option = flag.trim().replace(" ", "=")
          result.add(BazelFlag(option))
        }
      }
      return result
    }
  }
}

internal class QueryEvaluator(currentRunnerDirFile: VirtualFile) {
  private var bazelRunner: BazelRunner
  private var workspaceContext: WorkspaceContext

  init {
    currentRunnerDirFile.isDirectoryOrThrow()
    val getRunnerCallResult = getRunnerOfDirectory(currentRunnerDirFile)
    bazelRunner = getRunnerCallResult.first
    workspaceContext = getRunnerCallResult.second
  }

  private var currentProcess = AtomicReference<BazelProcess?>(null)
  private var currentProcessCancelled = AtomicBoolean(false)

  private fun getRunnerOfDirectory(directoryFile: VirtualFile): Pair<BazelRunner, WorkspaceContext> {
    directoryFile.isDirectoryOrThrow()

    val workspaceRoot = directoryFile.toNioPath()
    val emptyWorkspaceContext =
      WorkspaceContext(
        targets = emptyList(),
        directories = listOf(ExcludableValue.included(workspaceRoot)),
        buildFlags = emptyList(),
        syncFlags = emptyList(),
        debugFlags = emptyList(),
        bazelBinary = null,
        allowManualTargetsSync = false,
        dotBazelBspDirPath = workspaceRoot.resolve(".bazelbsp"),
        importDepth = 1,
        enabledRules = emptyList(),
        ideJavaHomeOverride = null,
        shardSync = false,
        targetShardSize = 1000,
        shardingApproach = null,
        importRunConfigurations = emptyList(),
        gazelleTarget = null,
        indexAllFilesInDirectories = false,
        pythonCodeGeneratorRuleNames = emptyList(),
        importIjars = false,
        deriveInstrumentationFilterFromTargets = false,
        indexAdditionalFilesInDirectories = emptyList(),
      )

    return Pair(BazelRunner(null, workspaceRoot), emptyWorkspaceContext)
  }

  // Starts a process which evaluates a given query.
  // Result is valid for one call of waitAndGetResult() and another evaluation cannot be called
  // before a result of previous is received.
  fun orderEvaluation(command: String, flags: List<BazelFlag>) {
    if (currentProcess.get() != null) throw IllegalStateException("Trying to start new process before result of previous is received")

    val commandToRun =
      bazelRunner.buildBazelCommand(workspaceContext) {
        queryExpression(command) { }
      }

    commandToRun.options.clear() // `BazelRunner.buildBazelCommand` adds some options of its own, we need to clear them
    for (flag in flags) {
      commandToRun.options.add(flag.value)
    }

    val startedProcess = bazelRunner.runBazelCommand(commandToRun, serverPidFuture = null)
    currentProcessCancelled.set(false)
    currentProcess.set(startedProcess)
  }

  // Cancels a currently running process
  fun cancelEvaluation() {
    val retrievedProcess = currentProcess.get()
    if (retrievedProcess != null) { // Cancellation might be called before UI changes state but after the process is finished
      if (currentProcessCancelled.compareAndSet(false, true)) {
        retrievedProcess.process.destroy() // it seems like it calls SIGTERM
      }
    }
  }

  // Returns process results if the process was not canceled, null otherwise
  suspend fun waitAndGetResults(): BazelProcessResult? {
    val retrievedProcess = currentProcess.get()
    return if (retrievedProcess != null) {
      val result = retrievedProcess.waitAndGetResult()
      currentProcess.set(null)
      retrievedProcess.process.destroy()
      if (currentProcessCancelled.get()) null else result
      // There exists an exit code in bazel (8) which would result with .bazelStatus in return value being BazelStatus.CANCEL,
      // but with SIGTERM being called on the process return value is 143, which results in BazelStatus.FATAL_ERROR.
      // Having a boolean which explicitly tells us if the process was canceled, while not ideal, works.
    } else {
      throw IllegalStateException("No command to get result from")
    }
  }
}

private fun VirtualFile.isDirectoryOrThrow() {
  if (!this.isDirectory) throw IllegalArgumentException("$this is not a directory")
}
