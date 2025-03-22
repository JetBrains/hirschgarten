package org.jetbrains.bazel.ui.dialogs.queryTab

import java.awt.BorderLayout
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JScrollPane

class GraphWindowManager {
  private var lastGraph: JFrame? = null

  fun openImageInNewWindow(image: ImageIcon) {
    lastGraph?.dispose()

    val graph = JFrame("Graph Visualization")
    graph.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
    graph.layout = BorderLayout()

    val imageLabel = JLabel(image)
    graph.add(JScrollPane(imageLabel), BorderLayout.CENTER)

    graph.setSize(800, 600)
    graph.setLocationRelativeTo(null)
    graph.isVisible = true

    lastGraph = graph
  }
}
