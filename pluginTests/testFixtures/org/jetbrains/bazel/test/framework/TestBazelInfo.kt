package org.jetbrains.bazel.test.framework

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelRelease
import java.nio.file.Path
import kotlin.io.path.Path

fun testBazelInfo(
  workspaceRoot: Path = Path("workspace"),
  outputBase: Path = Path("output-base"),
  execRoot: Path = outputBase.resolve("execroot").resolve("_main"),
): BazelInfo =
  BazelInfo(
    execRoot = execRoot,
    outputBase = outputBase,
    workspaceRoot = workspaceRoot,
    bazelBin = execRoot.resolve("bazel-bin"),
    release = BazelRelease.FALLBACK_VERSION,
    isBzlModEnabled = true,
    isWorkspaceEnabled = false,
    externalAutoloads = emptyList(),
  )
