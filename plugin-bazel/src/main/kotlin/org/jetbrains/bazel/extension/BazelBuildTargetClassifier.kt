package org.jetbrains.bazel.extension

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

private data class TargetIdComponents(val packagePath: String, val targetName: String)

internal class BazelBuildTargetClassifier : BuildTargetClassifierExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val separator: String = "/"

  override fun calculateBuildTargetPath(buildTargetInfo: BuildTargetInfo): List<String> =
    extractTargetIdComponents(buildTargetInfo.id.uri)
      ?.packagePath
      ?.split("/")
      ?.filter { it.isNotEmpty() }
      .orEmpty()

  override fun calculateBuildTargetName(buildTargetInfo: BuildTargetInfo): String =
    extractTargetIdComponents(buildTargetInfo.id.uri)
      ?.targetName
      ?: buildTargetInfo.id.uri

  /**
   * This method uses string manipulation to extract useful components from Bazel target id,
   * it assumes the target id has the format `@?@?(?<repository>.*)//(?<package>.*):(?<target>.*)`
   */
  private fun extractTargetIdComponents(label: String): TargetIdComponents? {
    val repositoryAndTheRest = label.split("//", limit = 2)
    if (repositoryAndTheRest.size != 2) return null
    val repository = repositoryAndTheRest[0].trimStart { it == '@' }
    val packagePathAndTargetName = repositoryAndTheRest[1].split(":", limit = 2)
    if (packagePathAndTargetName.isEmpty()) return null
    val fullPackagePath =
      if (repository.isEmpty()) {
        packagePathAndTargetName[0]
      } else {
        "$repository/${packagePathAndTargetName[0]}"
      }
    return if (packagePathAndTargetName.size == 1) {
      TargetIdComponents(
        packagePath = fullPackagePath,
        targetName = label,
      )
    } else {
      TargetIdComponents(
        packagePath = fullPackagePath,
        targetName = packagePathAndTargetName[1],
      )
    }
  }
}
