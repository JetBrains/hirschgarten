package org.jetbrains.bazel.workspace

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
interface BazelExecutableProvider {
  suspend fun computeBazelExecutable(project: Project): Path?

  companion object {
    val ep: ExtensionPointName<BazelExecutableProvider> = ExtensionPointName.create("org.jetbrains.bazel.bazelExecutableProvider")

    suspend fun computeBazelExecutableOrFail(project: Project): Path {
      for (provider in ep.extensionList) {
        val executable = provider.computeBazelExecutable(project)
        if (executable != null) {
          return executable
        }
      }
      error("Failed to find bazel executable")
    }
  }
}
