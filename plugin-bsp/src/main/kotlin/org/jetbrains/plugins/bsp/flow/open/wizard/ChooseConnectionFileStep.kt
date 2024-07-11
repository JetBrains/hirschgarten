package org.jetbrains.plugins.bsp.flow.open.wizard

import com.intellij.openapi.observable.properties.AtomicLazyProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bsp.protocol.BSP_CONNECTION_DIR

internal open class ChooseConnectionFileStep(
  projectPath: VirtualFile,
) : ImportProjectWizardStep() {
  private val allConnections = projectPath.findChild(BSP_CONNECTION_DIR)?.children.orEmpty()

  val connectionFile: ObservableMutableProperty<VirtualFile> =
    AtomicLazyProperty { allConnections.firstOrNull()
      ?: error("No connection file available. BSP plugin should not be available if there are no connection files.") }

  override val panel: DialogPanel = panel {
    row {
      panel {
        row {
          label("Available connection files:")
        }
        buttonsGroup {
          allConnections.map {
            row {
              radioButton(it.name, it)
            }
          }
        }.bind(
          { connectionFile.get() },
          { connectionFile.set(it) },
        )
      }
    }
  }
}
