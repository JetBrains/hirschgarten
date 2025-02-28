package org.jetbrains.bazel.server.connection

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bsp.protocol.JoinedBuildServer

/**
 * The BSP connection, implementation should keep all the information
 * needed to establish and keep the connection with the server.
 */
public interface BspConnection {
  /**
   * Executes a task on server, taking care of the connection to the server and
   * making sure that the newest available server is used (by calling [org.jetbrains.bazel.impl.server.connection.ConnectionDetailsProviderExtension.provideNewConnectionDetails])
   */
  public suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer) -> T): T
}

val Project.connection: BspConnection
  get() = BazelServerService.getInstance(this).connection

/**
 * the method should be solely used for mocking the project's BSP connection in tests.
 */
@TestOnly
fun Project.setMockTestConnection(newConnection: BspConnection) {
  BazelServerService.getInstance(this).connection = newConnection
}

@Service(Service.Level.PROJECT)
class BazelServerService(project: Project) {
  var connection: BspConnection = DefaultBspConnection(project)
    @Synchronized get

    @Synchronized internal set

  companion object {
    fun getInstance(project: Project): BazelServerService = project.getService(BazelServerService::class.java)
  }
}
