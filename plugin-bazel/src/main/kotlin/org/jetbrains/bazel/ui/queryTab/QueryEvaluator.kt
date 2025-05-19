package org.jetbrains.bazel.ui.queryTab

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.bazelrunner.BazelProcess
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.RelativeLabel
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.provider.WorkspaceContextConstructor
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

internal class QueryEvaluator(private var currentRunnerDirFile: VirtualFile) {
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

  fun setEvaluationDirectory(directoryFile: VirtualFile) {
    directoryFile.isDirectoryOrThrow()
    if (directoryFile == currentRunnerDirFile) return

    val getRunnerCallResult = getRunnerOfDirectory(directoryFile)
    bazelRunner = getRunnerCallResult.first
    workspaceContext = getRunnerCallResult.second
    currentRunnerDirFile = directoryFile
  }

  private fun getRunnerOfDirectory(directoryFile: VirtualFile): Pair<BazelRunner, WorkspaceContext> {
    directoryFile.isDirectoryOrThrow()

    val wcc = WorkspaceContextConstructor(directoryFile.toNioPath(), Path.of(""), Path.of(""))
    val pv = ProjectView.Builder().build()

    return Pair(BazelRunner(null, directoryFile.toNioPath()), wcc.construct(pv))
  }

  // Starts a process which evaluates given query.
  // Result is valid for one call of waitAndGetResult() and another evaluation cannot be called
  // before result of previous is received.
  fun orderEvaluation(command: String, flags: List<BazelFlag>) {
    if (currentProcess.get() != null) throw IllegalStateException("Trying to start new process before result of previous is received")

    // TODO: add proper way to add raw query to evaluation
    val label = RelativeLabel(Package(listOf(command)), AmbiguousEmptyTarget)
    val commandToRun =
      bazelRunner.buildBazelCommand(workspaceContext) {
        query { targets.add(label) }
      }

    commandToRun.options.clear()
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
    if (retrievedProcess != null) { // Cancellation might be called before UI changes state but after process is finished
      if (currentProcessCancelled.compareAndSet(false, true)) {
        retrievedProcess.process.destroy() // it seems like it calls SIGTERM
      }
    }
  }

  // Returns process results if process was not canceled, null otherwise
  suspend fun waitAndGetResults(): BazelProcessResult? {
    val retrievedProcess = currentProcess.get()
    return if (retrievedProcess != null) {
      val result = retrievedProcess.waitAndGetResult()
      currentProcess.set(null)
      retrievedProcess.process.destroy()
      if (currentProcessCancelled.get()) null else result
      // There exists an exit code in bazel (8) which would result with .bazelStatus in return value being BazelStatus.CANCEL,
      // but with SIGTERM being called on the process return value is 143, which results in BazelStatus.FATAL_ERROR.
      // Having a boolean which explicitely tells us if process was canceled, while not ideal, works.
    } else {
      throw IllegalStateException("No command to get result from")
    }
  }
}

private fun VirtualFile.isDirectoryOrThrow() {
  if (!this.isDirectory) throw IllegalArgumentException("$this is not a directory")
}
