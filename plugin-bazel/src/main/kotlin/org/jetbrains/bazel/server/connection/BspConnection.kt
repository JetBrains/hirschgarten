package org.jetbrains.bazel.server.connection

import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.JoinedBuildServer

/**
 * The BSP connection, implementation should keep all the information
 * needed to establish and keep the connection with the server.
 */
interface BspConnection {
  suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer) -> T): T
}

val Project.connection: BspConnection
  get() = BazelServerService.getInstance(this).connection

interface BazelServerService {
  val connection: BspConnection

  companion object {
    fun getInstance(project: Project): BazelServerService = project.getService(BazelServerService::class.java)
  }
}

class BazelServerServiceImpl(project: Project) : BazelServerService {
  override val connection: BspConnection by lazy { DefaultBspConnection(project) }
}
