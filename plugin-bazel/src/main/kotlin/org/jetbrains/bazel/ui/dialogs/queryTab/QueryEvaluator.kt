package org.jetbrains.bazel.ui.dialogs.queryTab

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.RelativeLabel
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.WorkspaceContextConstructor
import org.jetbrains.bazel.workspacecontext.WorkspaceContextProvider
import java.nio.file.Path

internal class QueryEvaluator {
  private var currentRunnerDirFile: VirtualFile? = null
  private var bazelRunner: BazelRunner? = null

  val isDirectorySet: Boolean get() = bazelRunner != null

  fun setEvaluationDirectory(directoryFile: VirtualFile) {
    if (!directoryFile.isDirectory) throw IllegalArgumentException("$directoryFile is not a directory")

    if (directoryFile == currentRunnerDirFile) {println("asd"); return}

    val wcc = WorkspaceContextConstructor(directoryFile.toNioPath(), Path.of(""))
    val pv = ProjectView.Builder().build()
    val wcp =
      object : WorkspaceContextProvider {
        private val wc = wcc.construct(pv)
        override fun currentWorkspaceContext(): WorkspaceContext = wc
      }
    bazelRunner = BazelRunner(wcp, null, directoryFile.toNioPath())

    currentRunnerDirFile = directoryFile
  }


  suspend fun evaluate(
    command: String,
    flags: List<String>,
    additionalFlags: String,
    flagsAlreadyPrefixedWithDoubleHyphen: Boolean = false
  ): BazelProcessResult {
    if (!isDirectorySet) throw IllegalStateException("Directory to run query from is not set")

    // TODO: add proper way to add raw query to evaluation
    val label = RelativeLabel(Package(listOf(command)), AmbiguousEmptyTarget)
    val commandToRun = bazelRunner!!.buildBazelCommand {
      query { targets.add(label) }
    }

    commandToRun.options.clear()
    for (flag in flags) {
      commandToRun.options.add((if (flagsAlreadyPrefixedWithDoubleHyphen) "" else "--") + flag)
    }
    for (flag in additionalFlags.split(" -")) {
      commandToRun.options.add(if (flag.startsWith("--")) flag.trim() else "-${flag.trim()}")
    }

    return bazelRunner!!
      .runBazelCommand(commandToRun, serverPidFuture = null)
      .waitAndGetResult()
  }
}
