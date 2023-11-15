package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.bsp.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.ChunkingBuildServer
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService

public abstract class BspServerTask<T>(private val taskName: String, protected val project: Project) {
  protected fun connectAndExecuteWithServer(task: (BspServer, BazelBuildServerCapabilities) -> T?): T? {
    val server = getServer()
    val capabilities = getCapabilities()
    return task(server, capabilities)
  }

  private fun getServer(): BspServer {
    val bspConnection = BspConnectionService.getInstance(project).value!!
    val server = bspConnection.server ?: connectToServer()
    return if (Registry.`is`("bsp.request.chunking.enable")) {
      val minChunkSize = Registry.intValue("bsp.request.chunking.size.min")
      ChunkingBuildServer(server, minChunkSize)
    } else server
  }

  private fun connectToServer(): BspServer {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    val bspConnection = BspConnectionService.getInstance(project).value!!

    bspSyncConsole.startTask(
      taskId = "bsp-autoconnect",
      title = BspPluginBundle.message("console.task.auto.connect.title"),
      message = BspPluginBundle.message("console.task.auto.connect.in.progress"),
    )

    return try {
      bspConnection.connect("bsp-autoconnect")
      bspConnection.server!!.also {
        bspSyncConsole.finishTask("bsp-autoconnect", BspPluginBundle.message("console.task.auto.connect.success"))
      }
    } catch (e: Exception) {
      bspSyncConsole.finishTask(
        "bsp-autoconnect",
        BspPluginBundle.message("console.task.auto.connect.failed"), FailureResultImpl(e)
      )
      error("Server connection failed!")
    }
  }

  private fun getCapabilities(): BazelBuildServerCapabilities =
    BspConnectionService.getInstance(project).value!!.capabilities
      ?: error("Unable to obtain server capabilities")
}

public abstract class BspServerSingleTargetTask<T>(taskName: String, project: Project) :
  BspServerTask<T>(taskName, project) {
  public fun connectAndExecute(targetId: BuildTargetIdentifier): T? =
    connectAndExecuteWithServer { server, capabilities -> executeWithServer(server, capabilities, targetId) }

  protected abstract fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): T
}

public abstract class BspServerMultipleTargetsTask<T>(taskName: String, project: Project) :
  BspServerSingleTargetTask<T>(taskName, project) {
  protected override fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): T = executeWithServer(server, capabilities, listOf(targetId))

  public fun connectAndExecute(targetsIds: List<BuildTargetIdentifier>): T? =
    connectAndExecuteWithServer { server, capabilities -> executeWithServer(server, capabilities, targetsIds) }

  protected abstract fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetsIds: List<BuildTargetIdentifier>,
  ): T
}
