package org.jetbrains.bazel.extension

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.extensionPoints.BuildTargetClassifierExtension
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

internal class BazelBuildTargetClassifier : BuildTargetClassifierExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val separator: String = "/"

  override fun calculateBuildTargetPath(buildTargetInfo: BuildTargetInfo): List<String> =
    Label
      .parse(buildTargetInfo.id.uri)
      .let { listOf((it as? ResolvedLabel)?.repoName.orEmpty()) + it.packagePath.pathSegments }
      .filter { pathSegment -> pathSegment.isNotEmpty() }

  override fun calculateBuildTargetName(buildTargetInfo: BuildTargetInfo): String = Label.parse(buildTargetInfo.id.uri).targetName
}
