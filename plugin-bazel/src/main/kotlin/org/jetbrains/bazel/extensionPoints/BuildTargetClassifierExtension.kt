package org.jetbrains.bazel.extensionPoints

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

public interface BuildTargetClassifierExtension {
  /**
   * Sets a separator for chaining target directories. Example:
   * ```
   * aaa
   *   | bbb
   *   | ccc
   *       | ddd
   *           | eee
   *               | BUILD_TARGET
   * ```
   * If the separator is set (for example to "+_"), the tree above will be rendered as:
   * ```
   * aaa
   *   | bbb
   *   | ccc+_ddd+_eee
   *       | BUILD_TARGET
   * ```
   * This affects only directories with just one child, this child being another directory
   */
  public val separator: String?

  /**
   * @param buildTargetInfo an intermediate representation of a BSP build target in `intellij-bsp`
   * @return list of directories corresponding to a desired path of the given build target in the tree.
   * For example, path `["aaa", "bbb"]` will render as:
   * ```
   * aaa
   *   | bbb
   *       | <buildTarget>
   * ```
   * If an empty list is returned, the build target will be rendered in the tree's top level
   */
  public fun calculateBuildTargetPath(buildTargetInfo: BuildTargetInfo): List<String>

  /**
   * @param buildTargetInfo an intermediate representation of a BSP build target in `intellij-bsp`
   * @return the name under which the given build target will be rendered in the tree
   */
  public fun calculateBuildTargetName(buildTargetInfo: BuildTargetInfo): String
}

/**
 * Default implementation of the [BuildTargetClassifierExtension] interface.
 * It will be used in BSP project and when no other implementation exists.
 */
object DefaultBuildTargetClassifierExtension : BuildTargetClassifierExtension {
  override val separator: String? = null

  override fun calculateBuildTargetPath(buildTargetInfo: BuildTargetInfo): List<String> = emptyList()

  override fun calculateBuildTargetName(buildTargetInfo: BuildTargetInfo): String = buildTargetInfo.id.uri
}

object BazelBuildTargetClassifier : BuildTargetClassifierExtension {
  override val separator: String = "/"

  override fun calculateBuildTargetPath(buildTargetInfo: BuildTargetInfo): List<String> =
    Label
      .parse(buildTargetInfo.id.uri)
      .let { listOf((it as? ResolvedLabel)?.repoName.orEmpty()) + it.packagePath.pathSegments }
      .filter { pathSegment -> pathSegment.isNotEmpty() }

  override fun calculateBuildTargetName(buildTargetInfo: BuildTargetInfo): String = Label.parse(buildTargetInfo.id.uri).targetName
}
