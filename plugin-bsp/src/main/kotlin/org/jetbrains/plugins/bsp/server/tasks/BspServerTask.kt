package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.connection.BspServer

public abstract class BspServerTask(protected val project: Project) {

  protected val server: BspServer
    get() = getServerOrThrow()

  // TODO im not sure about it - how we should handle case when the connection is not initialized / we are disconnected
  private fun getServerOrThrow(): BspServer {
    val connection = BspConnectionService.getInstance(project).value
    val server = connection.server

    if (server == null) {
      val exception = IllegalStateException("Server is null! You need to connect to the server before using it.")
      log.warn(exception)
      throw exception
    }

    return server
  }

  private companion object {
    private val log = logger<BspServerTask>()
  }
}

public abstract class BspServerSingleTargetTask<T>(project: Project) : BspServerTask(project) {

  public abstract fun execute(targetId: BuildTargetIdentifier): T
}

public abstract class BspServerMultipleTargetsTask<T>(project: Project) : BspServerSingleTargetTask<T>(project) {

  public override fun execute(targetId: BuildTargetIdentifier): T =
    execute(listOf(targetId))

  public abstract fun execute(targetsIds: List<BuildTargetIdentifier>): T
}
