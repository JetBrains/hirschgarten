package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import ch.epfl.scala.bsp4j.BuildTarget

public interface BspBuildTargetClassifier {
  /**
   * @return name of the tool corresponding to this classifier
   */
  public fun name(): String

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
  public fun separator(): String? = null

  /**
   * @param buildTarget a build target
   * @return list of directories corresponding to a desired path of the given build target in the tree.
   * For example, path `["aaa", "bbb"]` will render as:
   * ```
   * aaa
   *   | bbb
   *       | <buildTarget>
   * ```
   * If an empty list is returned, the build target will be rendered in the tree's top level
   */
  public fun getBuildTargetPath(buildTarget: BuildTarget): List<String>

  /**
   * @param buildTarget - a build target
   * @return the name under which the given build target will be rendered in the tree
   */
  public fun getBuildTargetName(buildTarget: BuildTarget): String = buildTarget.displayName
}

public class BspBuildTargetClassifierProvider(
  private val toolName: String?,
  private val bspBuildTargetClassifiers: List<BspBuildTargetClassifier>
) {
  private fun getExtensionOrNull(): BspBuildTargetClassifier? =
    bspBuildTargetClassifiers.firstOrNull { it.name() == toolName }

  public fun getSeparator(): String? =
    getExtensionOrNull()?.separator()

  public fun getBuildTargetPath(buildTarget: BuildTarget): List<String> =
    getExtensionOrNull()?.getBuildTargetPath(buildTarget) ?: listOf()

  public fun getBuildTargetName(buildTarget: BuildTarget): String =
    getExtensionOrNull()?.getBuildTargetName(buildTarget) ?: buildTarget.displayName
}
