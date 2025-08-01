package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.orLatestSupported
import java.nio.file.Paths
import kotlin.io.path.Path

object BazelPathsResolverMock {
  fun create(): BazelPathsResolver {
    val bazelInfo =
      BazelInfo(
        execRoot = Paths.get(""),
        outputBase = Paths.get(""),
        workspaceRoot = Paths.get(""),
        bazelBin = Path("bazel-bin"),
        release = BazelRelease.fromReleaseString("release 6.0.0").orLatestSupported(),
        false,
        true,
        emptyList(),
      )
    return BazelPathsResolver(bazelInfo)
  }
}
