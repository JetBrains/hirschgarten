package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import com.intellij.ui.components.JBLabel
import com.intellij.util.PlatformIcons
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import javax.swing.Icon
import javax.swing.SwingConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode

public sealed class TargetNode {
  /** Name to display on the rendered node */
  public abstract val displayName: String

  /** Icon to use when rendering the node */
  public abstract fun icon(iconProvider: Tristate<Icon>): Icon

  /** ID to copy after executing "Copy target ID" action or using a copy keyboard shortcut */
  public abstract fun idToCopy(): String

  public fun asMutableTreeNode(): MutableTreeNode = DefaultMutableTreeNode(this)

  public fun asComponent(
    iconProvider: Tristate<Icon>,
    labelHighlighter: (String) -> String,
  ): JBLabel = JBLabel(
    labelHighlighter(displayName),
    icon(iconProvider),
    SwingConstants.LEFT,
  )

  override fun toString(): String = this.idToCopy()

  public data class Directory(override val displayName: String) : TargetNode() {
    override fun icon(iconProvider: Tristate<Icon>): Icon =
      PlatformIcons.FOLDER_ICON

    override fun idToCopy(): String = displayName
  }

  public abstract class Target : TargetNode()

  public data class InvalidTarget(
    val id: BuildTargetId,
    val name: String,
  ) : Target() {
    override val displayName: String = name.strickenThrough()

    override fun icon(iconProvider: Tristate<Icon>): Icon =
      iconProvider.invalid

    override fun idToCopy(): String = id
  }

  public data class ValidTarget(
    val target: BuildTargetInfo,
    override val displayName: String,
    val isLoaded: Boolean,
  ) : Target() {
    override fun icon(iconProvider: Tristate<Icon>): Icon =
      if (isLoaded) iconProvider.loaded else iconProvider.unloaded

    override fun idToCopy(): String = target.id
  }
}

private fun String.strickenThrough(): String = "<html><s>$this</s><html>"
