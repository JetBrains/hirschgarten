package org.jetbrains.bazel.extensionPoints

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel

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
   * @param buildTarget an intermediate representation of a BSP build target in `intellij-bsp`
   * @return list of directories corresponding to a desired path of the given build target in the tree.
   * For example, path `["aaa", "bbb"]` will render as:
   * ```
   * aaa
   *   | bbb
   *       | <buildTarget>
   * ```
   * If an empty list is returned, the build target will be rendered in the tree's top level
   */
  public fun calculateBuildTargetPath(buildTarget: Label): List<String>

  /**
   * @param buildTarget an intermediate representation of a BSP build target in `intellij-bsp`
   * @return the name under which the given build target will be rendered in the tree
   */
  public fun calculateBuildTargetName(buildTarget: Label): String
}

/**
 * Default implementation of the [BuildTargetClassifierExtension] interface.
 * It will be used in BSP project and when no other implementation exists.
 */
object DefaultBuildTargetClassifierExtension : BuildTargetClassifierExtension {
  override val separator: String? = null

  override fun calculateBuildTargetPath(buildTarget: Label): List<String> = emptyList()

  override fun calculateBuildTargetName(buildTarget: Label): String = buildTarget.toShortString()
}

object BazelBuildTargetClassifier : BuildTargetClassifierExtension {
  override val separator: String = "/"

  override fun calculateBuildTargetPath(buildTarget: Label): List<String> =
    buildTarget
      .let { listOf((it as? ResolvedLabel)?.repoName.orEmpty()) + it.packagePath.pathSegments }
      .filter { pathSegment -> pathSegment.isNotEmpty() }

  override fun calculateBuildTargetName(buildTarget: Label): String = buildTarget.targetName
}
