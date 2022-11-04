package org.jetbrains.plugins.bsp.connection

import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.JavaBuildServer
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.services.UninitializedServiceVariableException


public interface BspServer : BuildServer, JavaBuildServer

/**
 * The BSP connection, implementation should keep all the information
 * needed to establish and keep the connection with the server.
 * Implementation shouldn't perform any heavy actions before [connect].
 */
public interface BspConnection {

  /**
   * BSP server instance, it should be used to query the server
   *
   * the variable should be *null* before [connect] call and after [disconnect]
   */
  public val server: BspServer?

  /**
   * Establish a connection with the server, and initialize [server].
   */
  public fun connect(taskId: Any)

  /**
   * Disconnect from the server,
   * perform cleanup actions (like killing the process, closing resources) and set [server] to *null*.
   */
  public fun disconnect()

  /**
   * Returns *true* if connection is active ([connect] was called, but [disconnect] wasn't)
   * and the connection (and the process) is alive. Otherwise *false*.
   */
  public fun isConnected(): Boolean
}

public data class BspConnectionState(
  public var bspFileConnectionState: BspFileConnectionState? = null,
  public var bspGeneratorConnectionState: BspGeneratorConnectionState? = null,
)

@State(
  name = "BspConnectionService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true
)
public class BspConnectionService(private val project: Project) : PersistentStateComponent<BspConnectionState> {

  public var connection: BspConnection? = null
    private set

  public fun init(connection: BspConnection) {
    this.connection = connection
  }

  override fun getState(): BspConnectionState? =
    when (val safeConnection = connection) {
      is BspFileConnection -> BspConnectionState(bspFileConnectionState = safeConnection.toState())
      is BspGeneratorConnection -> BspConnectionState(bspGeneratorConnectionState = safeConnection.toState())
      else -> null
    }

  override fun loadState(state: BspConnectionState) {
    connection = getConnection(project, state)
  }

  // TODO the !!
  private fun getConnection(project: Project, state: BspConnectionState): BspConnection? =
    when {
      state.bspFileConnectionState != null -> BspFileConnection.fromState(project, state.bspFileConnectionState!!)
      state.bspGeneratorConnectionState != null -> BspGeneratorConnection.fromState(
        project,
        state.bspGeneratorConnectionState!!
      )

      else -> null
    }

  public companion object {

    public fun getConnectionOrThrow(project: Project): BspConnection =
      when (val connection = getInstance(project).connection) {
        null -> throw UninitializedServiceVariableException(
          BspConnectionService::connection.name,
          BspConnectionService::class.simpleName
        )

        else -> connection
      }

    public fun getInstance(project: Project): BspConnectionService =
      project.getService(BspConnectionService::class.java)
  }
}
