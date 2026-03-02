package org.jetbrains.bazel.bazelrunner

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import java.nio.file.Path

@ApiStatus.Internal
interface BazelProcessLauncher {
  fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process
}

internal interface BazelProcessLauncherProvider {
  fun createBazelProcessLauncher(
    workspaceRoot: Path,
    bspInfo: BspInfo,
    aspectsResolver: InternalAspectsResolver,
    bazelInfoResolver: BazelInfoResolver,
  ): BazelProcessLauncher

  companion object {
    val ep = ExtensionPointName.create<BazelProcessLauncherProvider>("org.jetbrains.bazel.bazelProcessLauncherProvider")

    fun getInstance(): BazelProcessLauncherProvider =
      ep.extensionList.firstOrNull() ?: DefaultBazelProcessLauncherProvider
  }
}
