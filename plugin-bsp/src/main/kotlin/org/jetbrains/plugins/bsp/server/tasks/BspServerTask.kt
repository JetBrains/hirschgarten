package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.connection.BspServer

public abstract class BspServerTask<T>(private val taskName: String, protected val project: Project) {

  protected fun executeWithServerIfConnected(task: (BspServer) -> T?): T? {
    val server = getServer()

    return if (server != null) {
      task(server)
    } else {
      log.warn("Client is not connected to the server! Task '$taskName' can't be executed - skipping the task.")
      null
    }
  }

  private fun getServer(): BspServer? =
    BspConnectionService.getInstance(project).value.server

  private companion object {
    private val log = logger<BspServerTask<*>>()
  }
}

public abstract class BspServerSingleTargetTask<T>(taskName: String, project: Project) :
  BspServerTask<T>(taskName, project) {

  public fun executeIfConnected(targetId: BuildTargetIdentifier): T? =
    executeWithServerIfConnected { executeWithServer(it, targetId) }

  protected abstract fun executeWithServer(server: BspServer, targetId: BuildTargetIdentifier): T
}

public abstract class BspServerMultipleTargetsTask<T>(taskName: String, project: Project) :
  BspServerSingleTargetTask<T>(taskName, project) {

  protected override fun executeWithServer(server: BspServer, targetId: BuildTargetIdentifier): T =
    executeWithServer(server, listOf(targetId))

  public fun executeIfConnected(targetsIds: List<BuildTargetIdentifier>): T? =
    executeWithServerIfConnected { executeWithServer(it, targetsIds) }

  protected abstract fun executeWithServer(server: BspServer, targetsIds: List<BuildTargetIdentifier>): T
}
