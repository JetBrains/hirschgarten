package org.jetbrains.bazel.test.framework.testproject

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver
import org.jetbrains.bazel.workspace.model.test.framework.TEST_OUTPUT_ROOT
import org.jetbrains.bsp.protocol.OutputLocationParser
import org.jetbrains.bsp.protocol.OutputLocationResolver
import java.nio.file.Path
import kotlin.io.path.name

fun testProjectBazelInfo(
  workspaceRoot: Path,
  outputBase: Path = workspaceRoot.resolveSibling("${workspaceRoot.name}-output-base"),
  execRoot: Path = outputBase.resolve("execroot").resolve("_main"),
): BazelInfo =
  BazelInfo(
    execRoot = execRoot,
    outputBase = outputBase,
    workspaceRoot = workspaceRoot,
    bazelBin = TEST_OUTPUT_ROOT.segments.fold(execRoot.resolve("bazel-out"), Path::resolve),
    release = BazelRelease.FALLBACK_VERSION,
    isBzlModEnabled = true,
    isWorkspaceEnabled = false,
    externalAutoloads = emptyList(),
  )

fun BazelInfo.externalRepositoryPath(canonicalName: String): Path = outputBase.resolve("external").resolve(canonicalName)

fun BazelInfo.outputParser(): OutputLocationParser = OutputLocationParser(BazelPathsResolver(this), BazelOutFileHardLinks.NONE)

fun BazelInfo.outputResolver(): OutputLocationResolver =
  DefaultOutputLocationResolver.createHardlinkResolving(this, BazelOutFileHardLinks.NONE)
