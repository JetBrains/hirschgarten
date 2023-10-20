package org.jetbrains.bazel.extension

import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.extension.points.BspBuildTargetClassifierExtension

internal class BazelBuildTargetClassifier : BspBuildTargetClassifierExtension {
  override fun name(): String = "bazelbsp"

  override fun separator(): String = "/"

  private val bazelLabelRegex = """@?@?(?<repository>.*)//(?<package>.*):(?<target>.*)""".toRegex()

  override fun getBuildTargetPath(buildTargetIdentifier: BuildTargetId): List<String> {
    return bazelLabelRegex.find(buildTargetIdentifier)?.groups
      ?.get("package")
      ?.value
      ?.split("/")
      ?.filter { it.isNotEmpty() }
      .orEmpty()
  }

  override fun getBuildTargetName(buildTargetIdentifier: BuildTargetId): String =
    bazelLabelRegex.find(buildTargetIdentifier)?.groups?.get("target")?.value ?: buildTargetIdentifier
}
