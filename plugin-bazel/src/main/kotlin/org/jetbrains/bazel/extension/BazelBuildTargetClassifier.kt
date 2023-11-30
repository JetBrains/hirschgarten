package org.jetbrains.bazel.extension

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.extension.points.BuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.extension.points.BuildToolId

internal class BazelBuildTargetClassifier : BuildTargetClassifierExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val separator: String = "/"

  private val bazelLabelRegex = """@?@?(?<repository>.*)//(?<package>.*):(?<target>.*)""".toRegex()

  override fun calculateBuildTargetPath(buildTargetIdentifier: BuildTargetId): List<String> {
    return bazelLabelRegex.find(buildTargetIdentifier)?.groups
      ?.get("package")
      ?.value
      ?.split("/")
      ?.filter { it.isNotEmpty() }
      .orEmpty()
  }

  override fun calculateBuildTargetName(buildTargetIdentifier: BuildTargetId): String =
    bazelLabelRegex.find(buildTargetIdentifier)?.groups?.get("target")?.value ?: buildTargetIdentifier
}
