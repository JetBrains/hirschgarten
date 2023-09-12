package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension

import ch.epfl.scala.bsp4j.PackageFeatures
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspComponentInterface
import java.awt.event.MouseListener
import javax.swing.JPanel
import javax.swing.SwingConstants

public class BspPanelCargoFeaturesComponent private constructor(
  private val featuresTree: FeaturesTree,
) : JPanel(VerticalLayout(0)), BspComponentInterface {
  private val emptyTreeMessage = JBLabel(
    BspAllTargetsWidgetBundle.message("widget.no.features.message"),
    SwingConstants.CENTER,
  )

  public constructor(
    project: Project,
    packagesFeatures: Collection<PackageFeatures>,
  ) : this(FeaturesTree(project, packagesFeatures))

  init {
    updatePanelContent()
  }

  private fun updatePanelContent() {
    val newContent = when {
      featuresTree.isEmpty() -> emptyTreeMessage
      else -> featuresTree.treeComponent
    }
    this.add(newContent)
    this.revalidate()
    this.repaint()
  }

  public override fun wrappedInScrollPane(): JBScrollPane = JBScrollPane(this)

  /**
   * Adds a mouse listener to this panel's target tree and target search components
   *
   * @param listenerBuilder mouse listener builder
   */
  public fun addMouseListener(listenerBuilder: (FeaturesContainer) -> MouseListener) {
    featuresTree.addMouseListener(listenerBuilder)
  }
}
