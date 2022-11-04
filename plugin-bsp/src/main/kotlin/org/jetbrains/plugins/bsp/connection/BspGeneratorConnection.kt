package org.jetbrains.plugins.bsp.connection

import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.ConvertableToState
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.import.getProjectDirOrThrow
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetailsParser
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.ConsoleOutputStream

public data class BspGeneratorConnectionState(
  public var fileConnectionState: BspFileConnectionState? = null,
  public var generatorName: String? = null
)

public class BspGeneratorConnection : BspConnection, ConvertableToState<BspGeneratorConnectionState> {

  private val project: Project
  private val bspConnectionDetailsGenerator: BspConnectionDetailsGenerator

  private var fileConnection: BspFileConnection? = null

  public override val server: BspServer?
    get() = fileConnection?.server

  public constructor(
    project: Project,
    bspConnectionDetailsGenerator: BspConnectionDetailsGenerator
  ) {
    this.project = project
    this.bspConnectionDetailsGenerator = bspConnectionDetailsGenerator
  }

  private constructor(
    project: Project,
    state: BspGeneratorConnectionState
  ) {
    this.project = project
    this.fileConnection = state.fileConnectionState?.let { BspFileConnection.fromState(project, it) }

    this.bspConnectionDetailsGenerator =
      BspConnectionDetailsGeneratorExtension.extensions().first { it.name() == state.generatorName }
  }

  public override fun connect(taskId: Any) {
    if (fileConnection == null) {
      generateNewConnectionFile(taskId)
    }

    fileConnection?.connect(taskId)
  }

  public override fun disconnect() {
    fileConnection?.disconnect()
  }

  public override fun isConnected(): Boolean =
    fileConnection?.isConnected() == true

  override fun toState(): BspGeneratorConnectionState =
    BspGeneratorConnectionState(
      fileConnectionState = fileConnection?.toState(),
      generatorName = bspConnectionDetailsGenerator.name(),
    )

  public fun restart(taskId: Any) {
    disconnect()
    generateNewConnectionFile(taskId)
    connect(taskId)
  }

  private fun generateNewConnectionFile(taskId: Any) {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    val consoleOutputStream = ConsoleOutputStream(generateConnectionFileSubtaskId, bspSyncConsole)

    bspSyncConsole.startSubtask(taskId, generateConnectionFileSubtaskId, "BSP: Generating BSP connection details...")
    val connectionFile = bspConnectionDetailsGenerator.generateBspConnectionDetailsFile(
      project.getProjectDirOrThrow(),
      consoleOutputStream
    )
    // TODO
    val locatedBspConnectionDetails = LocatedBspConnectionDetailsParser.parseFromFile(connectionFile)!!
    fileConnection = BspFileConnection(project, locatedBspConnectionDetails)
    bspSyncConsole.finishSubtask(generateConnectionFileSubtaskId, "BSP: Generating BSP connection details done!")
  }

  public companion object {
    private const val generateConnectionFileSubtaskId = "bsp-generate-connection-file"

    public fun fromState(project: Project, state: BspGeneratorConnectionState): BspGeneratorConnection? =
      when (state.generatorName) {
        null -> null
        else -> BspGeneratorConnection(project, state)
      }
  }
}
