package org.jetbrains.bazel.ogRun.other
import java.awt.Component
import javax.swing.Box
import javax.swing.JComponent

// TODO: use JBBox
object UiUtil {
  fun createBox(vararg components: Component): Box = createBox(components.toList())

  /** Puts all the given components in order in a box, aligned left.  */
  fun createBox(components: Iterable<Component>): Box {
    val box: Box = Box.createVerticalBox()
    box.setAlignmentX(0f)
    for (component in components) {
      if (component is JComponent) {
        component.setAlignmentX(0f)
      }
      box.add(component)
    }
    return box
  }
}
