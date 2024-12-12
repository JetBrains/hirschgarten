package org.jetbrains.bazel.extension

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.extensionPoints.BuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

internal class BazelBuildTargetClassifier : BuildTargetClassifierExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val separator: String = "/"

  override fun calculateBuildTargetPath(buildTargetInfo: BuildTargetInfo): List<String> =
    Label
      .parse(buildTargetInfo.id.uri)
      .let { listOf(it.repoName) + it.packagePath.pathSegments }
      .filter { pathSegment -> pathSegment.isNotEmpty() }

  override fun calculateBuildTargetName(buildTargetInfo: BuildTargetInfo): String = Label.parse(buildTargetInfo.id.uri).targetName
}
