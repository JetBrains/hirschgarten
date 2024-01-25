package org.jetbrains.plugins.bsp.server.connection

import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.JavaBuildServer
import ch.epfl.scala.bsp4j.JvmBuildServer
import ch.epfl.scala.bsp4j.PythonBuildServer
import ch.epfl.scala.bsp4j.ScalaBuildServer
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.BazelBuildServer
import org.jetbrains.bsp.BazelBuildServerCapabilities

public interface BspServer :
  BuildServer,
  JavaBuildServer,
  JvmBuildServer,
  BazelBuildServer,
  PythonBuildServer,
  ScalaBuildServer

/**
 * The BSP connection, implementation should keep all the information
 * needed to establish and keep the connection with the server.
 */
public interface BspConnection {
  /**
   * Establish a connection with the server, and initialize server.
   * If the connection is already established no actions should be performed.
   */
  public fun connect(taskId: Any, errorCallback: () -> Unit = {})

  /**
   * Disconnect from the server,
   * perform cleanup actions (like killing the process, closing resources).
   */
  public fun disconnect()

  /**
   * Executes a task on server, taking care of the connection to the server and
   * making sure that the newest available server is used (by calling [ConnectionDetailsProviderExtension.provideNewConnectionDetails])
   */
  public fun <T> runWithServer(task: (server: BspServer, capabilities: BazelBuildServerCapabilities) -> T?): T?

  /**
   * Returns *true* if connection is active ([connect] was called, but [disconnect] wasn't)
   * and the connection (and the process) is alive. Otherwise *false*.
   */
  public fun isConnected(): Boolean
}

internal var Project.connection: BspConnection
  get() = findOrCreateConnection().also { connection = it }
  set(value) { BspConnectionService.getInstance(this).connection = value }

private fun Project.findOrCreateConnection(): BspConnection =
  BspConnectionService.getInstance(this).connection ?: DefaultBspConnection(this, connectionDetailsProvider)

@Service(Service.Level.PROJECT)
internal class BspConnectionService {
  var connection: BspConnection? = null

  internal companion object {
    fun getInstance(project: Project): BspConnectionService =
      project.getService(BspConnectionService::class.java)
  }
}
