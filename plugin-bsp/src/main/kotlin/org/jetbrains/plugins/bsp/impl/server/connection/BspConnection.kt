package org.jetbrains.plugins.bsp.impl.server.connection

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer

/**
 * The BSP connection, implementation should keep all the information
 * needed to establish and keep the connection with the server.
 */
public interface BspConnection {
  /**
   * Establish a connection with the server, and initialize server.
   * If the connection is already established no actions should be performed.
   */
  public suspend fun connect()

  /**
   * Disconnect from the server,
   * perform cleanup actions (like killing the process, closing resources).
   */
  public fun disconnect()

  /**
   * Executes a task on server, taking care of the connection to the server and
   * making sure that the newest available server is used (by calling [ConnectionDetailsProviderExtension.provideNewConnectionDetails])
   */
  public suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities) -> T): T

  /**
   * Returns *true* if connection is active ([connect] was called, but [disconnect] wasn't)
   * and the connection (and the process) is alive. Otherwise *false*.
   */
  public fun isConnected(): Boolean
}

val Project.connection: BspConnection
  get() = BspConnectionService.getInstance(this).connection

/**
 * the method should be solely used for mocking the project's BSP connection in tests.
 */
@TestOnly
fun Project.setMockTestConnection(newConnection: BspConnection) {
  BspConnectionService.getInstance(this).connection = newConnection
}

@Service(Service.Level.PROJECT)
private class BspConnectionService(project: Project) {
  var connection: BspConnection = DefaultBspConnection(project, project.connectionDetailsProvider)
    @Synchronized get

    @Synchronized set

  companion object {
    fun getInstance(project: Project): BspConnectionService = project.getService(BspConnectionService::class.java)
  }
}
