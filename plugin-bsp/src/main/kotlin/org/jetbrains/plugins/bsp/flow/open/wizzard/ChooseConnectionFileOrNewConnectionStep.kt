package org.jetbrains.plugins.bsp.flow.open.wizard

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import com.intellij.openapi.observable.properties.AtomicLazyProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.io.readText
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails

public sealed interface ConnectionFileOrNewConnection

public data class ConnectionFile(val locatedBspConnectionDetails: LocatedBspConnectionDetails) :
  ConnectionFileOrNewConnection

public object NewConnection : ConnectionFileOrNewConnection

public open class ChooseConnectionFileOrNewConnectionStep(
  private val projectPath: VirtualFile,
  private val isGeneratorAvailable: Boolean
) : ImportProjectWizardStep() {

  public val connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection> =
    AtomicLazyProperty { calculateDefaultConnectionFileOrNewConnectionProperty(projectPath) }

  private fun calculateDefaultConnectionFileOrNewConnectionProperty(projectPath: VirtualFile): ConnectionFileOrNewConnection {
    val firstConnectionFileOrNull = calculateAvailableConnectionFiles(projectPath).firstOrNull()

    return firstConnectionFileOrNull?.let { ConnectionFile(it) } ?: NewConnection
  }

  protected override val panel: DialogPanel = panel {
    row {
      panel {
        buttonsGroup {
          calculateAvailableConnectionFiles(projectPath).map {
            row {
              radioButton(calculateConnectionFileDisplayName(it), ConnectionFile(it))
                .comment(calculateConnectionFileComment(it))
            }
          }

          row {
            radioButton(newConnectionPrompt, NewConnection)
          }.visible(isGeneratorAvailable)
        }.bind({ connectionFileOrNewConnectionProperty.get() }, { connectionFileOrNewConnectionProperty.set(it) })
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

  private fun calculateConnectionFileDisplayName(locatedBspConnectionDetails: LocatedBspConnectionDetails): String {
    val parentDirName = locatedBspConnectionDetails.connectionFileLocation.parent.name
    val fileName = locatedBspConnectionDetails.connectionFileLocation.name

    return "Connection file: $parentDirName/$fileName"
  }

  private fun calculateConnectionFileComment(locatedBspConnectionDetails: LocatedBspConnectionDetails): String {
    val serverName = locatedBspConnectionDetails.bspConnectionDetails.name
    val serverVersion = locatedBspConnectionDetails.bspConnectionDetails.version
    val bspVersion = locatedBspConnectionDetails.bspConnectionDetails.bspVersion
    val supportedLanguages = locatedBspConnectionDetails.bspConnectionDetails.languages.joinToString(", ")

    return """
      |Server name: $serverName$htmlBreakLine
      |Server version: $serverVersion$htmlBreakLine
      |BSP version: $bspVersion$htmlBreakLine
      |Supported languages: $supportedLanguages$htmlBreakLine
    """.trimMargin()
  }

  private companion object {
    private const val newConnectionPrompt = "New Connection"
    private const val htmlBreakLine = "<br>"
  }
}
