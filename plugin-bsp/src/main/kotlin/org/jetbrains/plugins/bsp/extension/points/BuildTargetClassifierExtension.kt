package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo

public interface BuildTargetClassifierExtension : WithBuildToolId {
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

  public companion object {
    internal val ep =
      ExtensionPointName.create<BuildTargetClassifierExtension>("org.jetbrains.bsp.buildTargetClassifierExtension")
  }
}

/**
 * Default implementation of the [BuildTargetClassifierExtension] interface.
 * It will be used in BSP project and when no other implementation exists.
 */
internal class DefaultBuildTargetClassifierExtension : BuildTargetClassifierExtension {
  override val buildToolId: BuildToolId = bspBuildToolId

  override val separator: String? = null

  override fun calculateBuildTargetPath(buildTargetInfo: BuildTargetInfo): List<String> = emptyList()

  override fun calculateBuildTargetName(buildTargetInfo: BuildTargetInfo): String = buildTargetInfo.id
}
