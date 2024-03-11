package org.jetbrains.bazel.extension

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo

internal class BazelBuildTargetClassifier : BuildTargetClassifierExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val separator: String = "/"

  private val bazelLabelRegex = """@?@?(?<repository>.*)//(?<package>.*):(?<target>.*)""".toRegex()

  override fun calculateBuildTargetPath(buildTargetInfo: BuildTargetInfo): List<String> {
    return bazelLabelRegex.find(buildTargetInfo.id)?.groups
      ?.get("package")
      ?.value
      ?.split("/")
      ?.filter { it.isNotEmpty() }
      .orEmpty()
  }

  override fun calculateBuildTargetName(buildTargetInfo: BuildTargetInfo): String =
    bazelLabelRegex.find(buildTargetInfo.id)?.groups?.get("target")?.value ?: buildTargetInfo.id
}
