package org.jetbrains.bazel.bazelrunner

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
interface BazelProcessLauncher {
  fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process
}

@ApiStatus.Internal
interface BazelProcessLauncherProvider {
  fun createBazelProcessLauncher(
    workspaceRoot: Path,
    parentEnvironment: Map<String, String>,
  ): BazelProcessLauncher

  companion object {
    val ep = ExtensionPointName.create<BazelProcessLauncherProvider>("org.jetbrains.bazel.bazelProcessLauncherProvider")

    fun getInstance(): BazelProcessLauncherProvider =
      ep.extensionList.firstOrNull() ?: DefaultBazelProcessLauncherProvider
  }
}
