package org.jetbrains.plugins.bsp.flow.open.wizard

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.observable.properties.AtomicLazyProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetailsParser

private data class ConnectionChoiceContainer(var connection: ConnectionFileOrNewConnection)

public open class ChooseConnectionFileOrNewConnectionStep(
  private val projectPath: VirtualFile,
  private val availableGenerators: List<BspConnectionDetailsGenerator>,
  private val onChoiceChange: () -> Unit
) : ImportProjectWizardStep() {

  private val allAvailableConnectionMap by lazy { calculateAllAvailableConnections() }
  private val log = logger<ChooseConnectionFileOrNewConnectionStep>()

  private fun calculateAllAvailableConnections(): Map<String, List<ConnectionFileOrNewConnection>> {
    val connectionsFromFiles = calculateAvailableConnectionFiles(projectPath)
    val connectionsFromGenerators = availableGenerators.map { NewConnection(it) }
    return (connectionsFromFiles + connectionsFromGenerators).groupBy { it.id }
  }

  public val connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection> =
    AtomicLazyProperty { calculateDefaultConnectionFileOrNewConnection() }

  private fun calculateDefaultConnectionFileOrNewConnection(): ConnectionFileOrNewConnection {
    val allConnections = allAvailableConnectionMap.values.flatten()

    return when {
      canBeSkipped() -> calculateDefaultConnection(allConnections)
      else -> allConnections.first()
    }
  }

  private fun calculateDefaultConnection(
    allConnections: List<ConnectionFileOrNewConnection>): ConnectionFileOrNewConnection {
    val newestConnectionFile = allConnections.filterIsInstance<ConnectionFile>().maxOrNull()

    return newestConnectionFile ?: allConnections.filterIsInstance<NewConnection>().first()
  }

  public fun canBeSkipped(): Boolean =
    Registry.`is`("bsp.wizard.choose.default.connection") && allAvailableConnectionMap.size == 1

  protected override val panel: DialogPanel = panel {
    row {
      panel {
        buttonsGroup {
          allAvailableConnectionMap.entries.toList().map { generateButtonsGroupRow(it) }
        }.bind(
          { ConnectionChoiceContainer(connectionFileOrNewConnectionProperty.get()) },
          { connectionFileOrNewConnectionProperty.set(it.connection) }
        )
      }
    }
  }

  private fun calculateAvailableConnectionFiles(projectPath: VirtualFile): List<ConnectionFile> =
    projectPath.findChild(".bsp")
      ?.children
      .orEmpty()
      .toList()
      .filter { it.extension == "json" }
      .mapNotNull { parseConnectionFile(it) }

  private fun parseConnectionFile(virtualFile: VirtualFile): ConnectionFile? {
    val connectionDetails = LocatedBspConnectionDetailsParser.parseFromFile(virtualFile)
    if (connectionDetails.bspConnectionDetails == null) {
      log.error("Parsing file '$virtualFile' to ConnectionFile failed!\n")
      return null
    }

    return ConnectionFile(
      bspConnectionDetails = connectionDetails.bspConnectionDetails,
      connectionFile = connectionDetails.connectionFileLocation
    )
  }

  private fun Panel.generateButtonsGroupRow(
    connectionEntry: Map.Entry<String, List<ConnectionFileOrNewConnection>>
  ) {
    val connections = connectionEntry.value
    if (connections.isNotEmpty()) {
      val choiceContainer = ConnectionChoiceContainer(connections.first())
      row {
        val radioButton = radioButton(choiceContainer.connection.displayName, choiceContainer)
        radioButton.component.addActionListener {
          panel.apply()
          onChoiceChange()
        }
        generateRadioButtonLabel(connections, choiceContainer, radioButton)
      }
    }
  }

  private fun Row.generateRadioButtonLabel(
    connections: List<ConnectionFileOrNewConnection>,
    choiceContainer: ConnectionChoiceContainer,
    radioButton: Cell<JBRadioButton>
  ) {
    when {
      connections.size == 1 && connections.first() is NewConnection ->
        label("No connection files. New file will be created.")

      connections.size == 1 && connections.first() is ConnectionFile -> {
        val connectionFile = connections.first() as ConnectionFile
        label("Connection file: $connectionFile")
      }

      else -> {
        label("Connection file: ")
        val dropDown = dropDownLink(connections.first(), connections)
        dropDown.onChanged {
          choiceContainer.connection = it.selectedItem
          radioButton.component.doClick()
        }
      }
    }
  }
}
