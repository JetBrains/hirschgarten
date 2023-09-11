package org.jetbrains.plugins.bsp.server.connection

import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.CargoBuildServer
import ch.epfl.scala.bsp4j.JavaBuildServer
import ch.epfl.scala.bsp4j.JvmBuildServer
import ch.epfl.scala.bsp4j.PythonBuildServer
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.magicmetamodel.BazelBuildServer

public interface BspServer : BuildServer, CargoBuildServer, JavaBuildServer, BazelBuildServer, PythonBuildServer

/**
 * The BSP connection, implementation should keep all the information
 * needed to establish and keep the connection with the server.
 * Implementation shouldn't perform any heavy actions before [connect].
 */
public interface BspConnection {
  /**
   * ID of bsp connection's build tool
   */
  public val buildToolId: String?

  /**
   * BSP server instance, it should be used to query the server
   *
   * the variable should be *null* before [connect] call and after [disconnect]
   */
  public val server: BspServer?

  /**
   * BSP server capabilities
   *
   * the variable should be *null* before [connect] call and after [disconnect]
   */
  public val capabilities: BuildServerCapabilities?

  /**
   * Establish a connection with the server, and initialize [server].
   * If the connection is already established no actions should be performed.
   */
  public fun connect(taskId: Any, errorCallback: () -> Unit = {})

  public fun cargoFeaturesPostConnectAction(parentTaskId: Any)

  /**
   * Disconnect from the server,
   * perform cleanup actions (like killing the process, closing resources) and set [server] to *null*.
   */
  public fun disconnect()

  /**
   * Reloads the state of the connection. If server is disconnected it should reload (reparse) the connection file.
   */
  public fun reload()

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
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
@ApiStatus.Internal
public class BspConnectionService(private val project: Project) :
  PersistentStateComponent<BspConnectionState> {
  public var value: BspConnection? = null

  override fun getState(): BspConnectionState? =
    when (val safeConnection = value) {
      is BspFileConnection -> BspConnectionState(bspFileConnectionState = safeConnection.toState())
      is BspGeneratorConnection -> BspConnectionState(bspGeneratorConnectionState = safeConnection.toState())
      else -> null
    }

  override fun loadState(state: BspConnectionState) {
    value = getConnection(project, state)
  }

  private fun getConnection(project: Project, state: BspConnectionState): BspConnection? =
    state.bspFileConnectionState?.let { BspFileConnection.fromState(project, it) }
      ?: state.bspGeneratorConnectionState?.toValidGeneratorConnection(project)

  private fun BspGeneratorConnectionState.toValidGeneratorConnection(project: Project): BspConnection? {
    val generatorConnection = BspGeneratorConnection.fromState(project, this)
    return when (generatorConnection?.hasFileConnectionDefined()) {
      true -> generatorConnection
      else -> null
    }
  }

  public companion object {
    public fun getInstance(project: Project): BspConnectionService =
      project.getService(BspConnectionService::class.java)
  }
}
