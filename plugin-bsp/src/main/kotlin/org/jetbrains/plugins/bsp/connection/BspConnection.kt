package org.jetbrains.plugins.bsp.connection

import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.JavaBuildServer
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project

public interface BspServer : BuildServer, JavaBuildServer

public interface BspConnection {

  public val server: BspServer?

  public fun connect()

  public fun disconnect()

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
    when(val safeConnection = connection) {
      is BspFileConnection -> BspConnectionState(bspFileConnectionState = safeConnection.toState())
      is BspGeneratorConnection -> BspConnectionState(bspGeneratorConnectionState = safeConnection.toState())
      else -> null
  }

  override fun loadState(state: BspConnectionState) {
    connection = getConnection(project, state)
  }

  private fun getConnection(project: Project, state: BspConnectionState): BspConnection? =
    when {
      state.bspFileConnectionState != null -> BspFileConnection.fromState(project, state.bspFileConnectionState!!)
      state.bspGeneratorConnectionState != null -> BspGeneratorConnection.fromState(project, state.bspGeneratorConnectionState!!)
      else -> null
    }

  public companion object {
    public fun getInstance(project: Project): BspConnectionService =
      project.getService(BspConnectionService::class.java)
  }
}
