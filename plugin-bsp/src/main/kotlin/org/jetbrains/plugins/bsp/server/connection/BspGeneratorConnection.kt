package org.jetbrains.plugins.bsp.server.connection

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.BazelBuildServerCapabilities
import org.jetbrains.magicmetamodel.impl.ConvertableToState
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetailsParser
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.ConsoleOutputStream

public data class BspGeneratorConnectionState(
  public var fileConnectionState: BspFileConnectionState? = null,
  public var generatorId: String? = null,
)

public class BspGeneratorConnection : BspConnection, ConvertableToState<BspGeneratorConnectionState> {
  private val project: Project
  private val bspConnectionDetailsGenerator: BspConnectionDetailsGenerator

  private var fileConnection: BspFileConnection? = null

  public override val buildToolId: String?

  public override val server: BspServer?
    get() = fileConnection?.server

  public override val capabilities: BazelBuildServerCapabilities?
    get() = fileConnection?.capabilities

  public constructor(
    project: Project,
    bspConnectionDetailsGenerator: BspConnectionDetailsGenerator,
  ) {
    this.project = project
    this.bspConnectionDetailsGenerator = bspConnectionDetailsGenerator
    this.buildToolId = bspConnectionDetailsGenerator.id()
  }

  private constructor(
    project: Project,
    state: BspGeneratorConnectionState,
  ) {
    this.project = project
    this.fileConnection = state.fileConnectionState?.let { BspFileConnection.fromState(project, it) }

    this.bspConnectionDetailsGenerator =
      BspConnectionDetailsGeneratorExtension.extensions().first { it.id() == state.generatorId }

    this.buildToolId = state.generatorId
  }

  public override fun connect(taskId: Any, errorCallback: () -> Unit) {
    if (fileConnection == null) {
      generateNewConnectionFile(taskId)
    }

    fileConnection?.connect(taskId, errorCallback)
  }

  public override fun disconnect() {
    fileConnection?.disconnect()
  }

  override fun reload() {
    fileConnection?.reload()
  }

  public override fun isConnected(): Boolean =
    fileConnection?.isConnected() == true

  public fun hasFileConnectionDefined(): Boolean =
    fileConnection != null

  public fun getLocatedBspConnectionDetails(): LocatedBspConnectionDetails? =
    fileConnection?.locatedConnectionFile

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
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    val consoleOutputStream = ConsoleOutputStream(generateConnectionFileSubtaskId, bspSyncConsole)

    bspSyncConsole.startSubtask(
      taskId, generateConnectionFileSubtaskId,
      BspPluginBundle.message("console.task.generate.connection.file.in.progress")
    )

    try {
      val connectionFile = bspConnectionDetailsGenerator.generateBspConnectionDetailsFile(
        project.rootDir,
        consoleOutputStream,
        project,
      )
      val locatedBspConnectionDetails = LocatedBspConnectionDetailsParser.parseFromFile(connectionFile)!!
      fileConnection = BspFileConnection(project, locatedBspConnectionDetails)
      bspSyncConsole.finishSubtask(
        generateConnectionFileSubtaskId,
        BspPluginBundle.message("console.task.generate.connection.file.success")
      )
    } catch (e: Exception) {
      bspSyncConsole.finishSubtask(
        subtaskId = generateConnectionFileSubtaskId,
        message = BspPluginBundle.message("console.task.generate.connection.file.failed"),
        result = FailureResultImpl(e),
      )
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
