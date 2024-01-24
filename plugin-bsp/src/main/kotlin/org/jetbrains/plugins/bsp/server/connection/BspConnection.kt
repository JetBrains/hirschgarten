package org.jetbrains.plugins.bsp.server.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.JavaBuildServer
import ch.epfl.scala.bsp4j.JvmBuildServer
import ch.epfl.scala.bsp4j.PythonBuildServer
import ch.epfl.scala.bsp4j.ScalaBuildServer
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bsp.BazelBuildServer
import org.jetbrains.bsp.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.WithBuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault

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

/**
 * Extension that provides connection details for connecting to a BSP server.
 *
 * It should provide the latest available connection details so
 * the client is always connected to the latest server.
 * For example if the connection file has changed after initial import of the project,
 * this extension allows the client to disconnect from the old server and connection to the new one.
 *
 * Implementation should take care of its state (e.g. using a dedicated service with state).
 */
public interface ConnectionDetailsProviderExtension : WithBuildToolId {
  /**
   * Method called only on the first opening of the project (so initial sync).
   * It should be used to run an initial configuration, e.g. show a wizard.
   *
   * Note: UI actions should be executed under: [com.intellij.openapi.application.writeAction]
   *
   * @return [true] if all the actions have succeeded and initial sync should continue;
   * [false] if something has failed and initial sync should be terminated.
   */
  public suspend fun onFirstOpening(project: Project, projectPath: VirtualFile): Boolean

  /**
   * Provides the new connection details if are available.
   *
   * The method is called before each task executed on server.
   * Client will disconnect from the old server and connect to the new one if non-null value is returned.
   *
   * [currentConnectionDetails] set to [null] means that client is not aware of any connection details
   * (like during initial sync or after reopening the project)
   *
   * @return [null] if the newest available connection details are equal to [currentConnectionDetails];
   * otherwise new [BspConnectionDetails] if available
   */
  public fun provideNewConnectionDetails(
    project: Project,
    currentConnectionDetails: BspConnectionDetails?,
  ): BspConnectionDetails?

  public companion object {
    internal val ep: ExtensionPointName<ConnectionDetailsProviderExtension> =
      ExtensionPointName.create("org.jetbrains.bsp.connectionDetailsProviderExtension")
  }
}

internal var Project.connection: BspConnection
  get() = findOrCreateConnection().also { connection = it }
  set(value) { BspConnectionService.getInstance(this).connection = value }

private fun Project.findOrCreateConnection(): BspConnection =
  BspConnectionService.getInstance(this).connection ?: createNewConnection()

private fun Project.createNewConnection(): BspConnection {
  val extension = ConnectionDetailsProviderExtension.ep.withBuildToolIdOrDefault(buildToolId)

  return DefaultBspConnection(this, extension)
}

@Service(Service.Level.PROJECT)
internal class BspConnectionService {
  var connection: BspConnection? = null

  internal companion object {
    fun getInstance(project: Project): BspConnectionService =
      project.getService(BspConnectionService::class.java)
  }
}
