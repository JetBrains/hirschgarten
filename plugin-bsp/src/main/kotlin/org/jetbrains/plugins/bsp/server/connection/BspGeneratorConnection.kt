package org.jetbrains.plugins.bsp.server.connection

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.ConvertableToState
import org.jetbrains.plugins.bsp.config.ProjectPropertiesService
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetailsParser
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.ConsoleOutputStream

public data class BspGeneratorConnectionState(
  public var fileConnectionState: BspFileConnectionState? = null,
  public var generatorId: String? = null
)

public class BspGeneratorConnection : BspConnection, ConvertableToState<BspGeneratorConnectionState> {

  private val project: Project
  private val bspConnectionDetailsGenerator: BspConnectionDetailsGenerator

  private var fileConnection: BspFileConnection? = null

  public override val buildToolId: String?

  public override val server: BspServer?
    get() = fileConnection?.server

  public constructor(
    project: Project,
    bspConnectionDetailsGenerator: BspConnectionDetailsGenerator
  ) {
    this.project = project
    this.bspConnectionDetailsGenerator = bspConnectionDetailsGenerator
    this.buildToolId = bspConnectionDetailsGenerator.id()
  }

  private constructor(
    project: Project,
    state: BspGeneratorConnectionState
  ) {
    this.project = project
    this.fileConnection = state.fileConnectionState?.let { BspFileConnection.fromState(project, it) }

    this.bspConnectionDetailsGenerator =
      BspConnectionDetailsGeneratorExtension.extensions().first { it.id() == state.generatorId }

    this.buildToolId = state.generatorId
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
      generatorId = bspConnectionDetailsGenerator.id(),
    )

  public fun restart(taskId: Any) {
    disconnect()
    generateNewConnectionFile(taskId)
    connect(taskId)
  }

  private fun generateNewConnectionFile(taskId: Any) {
    val projectProperties = ProjectPropertiesService.getInstance(project).value

    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    val consoleOutputStream = ConsoleOutputStream(generateConnectionFileSubtaskId, bspSyncConsole)

    bspSyncConsole.startSubtask(taskId, generateConnectionFileSubtaskId, "Generating BSP connection details...")

    try {
      val connectionFile = bspConnectionDetailsGenerator.generateBspConnectionDetailsFile(
        projectProperties.projectRootDir,
        consoleOutputStream
      )
      // TODO
      val locatedBspConnectionDetails = LocatedBspConnectionDetailsParser.parseFromFile(connectionFile)!!
      fileConnection = BspFileConnection(project, locatedBspConnectionDetails)
      bspSyncConsole.finishSubtask(generateConnectionFileSubtaskId, "Generating BSP connection details done!")
    } catch (e: Exception) {
      bspSyncConsole.finishSubtask(generateConnectionFileSubtaskId, "Generating BSP connection details failed!", FailureResultImpl(e))
    }
  }

  public companion object {
    private const val generateConnectionFileSubtaskId = "bsp-generate-connection-file"

    public fun fromState(project: Project, state: BspGeneratorConnectionState): BspGeneratorConnection? =
      when (state.generatorId) {
        null -> null
        else -> BspGeneratorConnection(project, state)
      }
  }
}
