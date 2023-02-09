package org.jetbrains.plugins.bsp.flow.open.wizard

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import com.intellij.openapi.observable.properties.AtomicLazyProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.intellij.util.io.readText
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails

private data class ConnectionChoiceContainer(var connection: ConnectionFileOrNewConnection)

public open class ChooseConnectionFileOrNewConnectionStep(
  private val projectPath: VirtualFile,
  private val availableGenerators: List<BspConnectionDetailsGenerator>,
  private val onChoiceChange: () -> Unit
) : ImportProjectWizardStep() {

  private val allAvailableConnectionList by lazy { calculateAllAvailableConnections().entries.toList() }

  private fun calculateAllAvailableConnections(): Map<String, List<ConnectionFileOrNewConnection>> {
    val connectionFiles = calculateAvailableConnectionFiles(projectPath)
    val connectionsFromFiles = connectionFiles.map { ConnectionFile(it) }
    val connectionsFromGenerators = availableGenerators.map { NewConnection(it) }
    return (connectionsFromFiles + connectionsFromGenerators).groupBy { it.connectionName }
  }

  public val connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection> =
    AtomicLazyProperty { allAvailableConnectionList.first().value.first() }

  protected override val panel: DialogPanel = panel {
    row {
      panel {
        buttonsGroup {
          allAvailableConnectionList.map { generateButtonsGroupRow(it) }
        }.bind(
          { ConnectionChoiceContainer(connectionFileOrNewConnectionProperty.get()) },
          { connectionFileOrNewConnectionProperty.set(it.connection) }
        )
      }
    }
  }

  private fun calculateAvailableConnectionFiles(projectPath: VirtualFile): List<LocatedBspConnectionDetails> =
    projectPath.findChild(".bsp")
      ?.children
      .orEmpty().toList()
      .filter { it.extension == "json" }
      .map { parseConnectionFile(it) }

  private fun parseConnectionFile(virtualFile: VirtualFile): LocatedBspConnectionDetails {
    val fileContent = virtualFile.toNioPath().readText()

    return LocatedBspConnectionDetails(
      bspConnectionDetails = Gson().fromJson(fileContent, BspConnectionDetails::class.java),
      connectionFileLocation = virtualFile,
    )
  }

  private fun Panel.generateButtonsGroupRow(
    connectionEntry: Map.Entry<String, List<ConnectionFileOrNewConnection>>
  ) {
    val connections = connectionEntry.value
    if (connections.isNotEmpty()) {
      val choiceContainer = ConnectionChoiceContainer(connections.first())
      row {
        val radioButton = radioButton(connectionEntry.key, choiceContainer)
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
      connections.size == 1 && connections.first() is NewConnection -> {
        label("No connection files. New file will be created.")
      }
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
