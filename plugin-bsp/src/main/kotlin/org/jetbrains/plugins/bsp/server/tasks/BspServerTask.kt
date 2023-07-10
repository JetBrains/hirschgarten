package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.plugins.bsp.server.ChunkingBuildServer
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.ConnectAction

public abstract class BspServerTask<T>(private val taskName: String, protected val project: Project) {

  protected fun connectAndExecuteWithServer(task: (BspServer) -> T?): T? {
    var server = getServer()
    return if (server == null) {
      BspCoroutineService
        .getInstance(project)
        .start { ConnectAction.connectCoroutine(project) }
        .asCompletableFuture()
        .get()
      server = getServer()
      if (server != null) {
        task(server)
      } else {
        log.warn("Client is not connected to the server! Task '$taskName' can't be executed - skipping the task.")
        null
      }
    } else {
      task(server)
    }
  }

  private fun getServer(): BspServer? {
    val server = BspConnectionService.getInstance(project).value!!.server
    return if (server == null) null
    else if (Registry.`is`("bsp.request.chunking.enable")) {
      val minChunkSize = Registry.intValue("bsp.request.chunking.size.min")
      ChunkingBuildServer(server, minChunkSize)
    } else server
  }

  private companion object {
    private val log = logger<BspServerTask<*>>()
  }
}

public abstract class BspServerSingleTargetTask<T>(taskName: String, project: Project) :
  BspServerTask<T>(taskName, project) {

  public fun connectAndExecute(targetId: BuildTargetIdentifier): T? =
    connectAndExecuteWithServer { executeWithServer(it, targetId) }

  protected abstract fun executeWithServer(server: BspServer, targetId: BuildTargetIdentifier): T
}

public abstract class BspServerMultipleTargetsTask<T>(taskName: String, project: Project) :
  BspServerSingleTargetTask<T>(taskName, project) {

  protected override fun executeWithServer(server: BspServer, targetId: BuildTargetIdentifier): T =
    executeWithServer(server, listOf(targetId))

  public fun connectAndExecute(targetsIds: List<BuildTargetIdentifier>): T? =
    connectAndExecuteWithServer { executeWithServer(it, targetsIds) }

  protected abstract fun executeWithServer(server: BspServer, targetsIds: List<BuildTargetIdentifier>): T
}
