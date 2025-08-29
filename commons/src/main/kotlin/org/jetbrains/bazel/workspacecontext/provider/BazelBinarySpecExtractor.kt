package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.ExecUtils
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContextEntityExtractorException
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object BazelBinarySpecExtractor : WorkspaceContextEntityExtractor<BazelBinarySpec> {
  override fun fromProjectView(projectView: ProjectView): BazelBinarySpec {
    val extracted = projectView.bazelBinary?.value
    return if (extracted != null) {
      BazelBinarySpec(extracted)
    } else {
      val path =
        findBazelOnPathOrNull()
          ?: throw WorkspaceContextEntityExtractorException(
            "bazel path",
            "Bazel binary not found in PATH. " +
              "Please set the 'bazel_binary' field in your project view or ensure that Bazel is installed and available in your PATH.",
          )
      BazelBinarySpec(path)
    }
  }

  private fun findBazelOnPathOrNull(): Path? =
    splitPath()
      .flatMap { listOf(bazelFile(it, "bazel"), bazelFile(it, "bazelisk")) }
      .filterNotNull()
      .firstOrNull()

  private fun splitPath(): List<String> {
    val environmentProvider = EnvironmentProvider.getInstance()
    return environmentProvider.getValue("PATH")?.split(File.pathSeparator).orEmpty()
  }

  private fun bazelFile(path: String, executable: String): Path? {
    val file = File(path, ExecUtils.calculateExecutableName(executable))
    return if (file.exists() && file.canExecute()) file.toPath() else null
  }
}
