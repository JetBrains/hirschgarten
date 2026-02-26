package org.jetbrains.bazel.server.connection

import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BazelServerFacade

/**
 * The BSP connection, implementation should keep all the information
 * needed to establish and keep the connection with the server.
 */
interface BazelServerConnection {
  suspend fun <T> runWithServer(task: suspend (server: BazelServerFacade) -> T): T
}

val Project.connection: BazelServerConnection
  get() = BazelServerService.getInstance(this).connection

interface BazelServerService {
  val connection: BazelServerConnection

  companion object {
    fun getInstance(project: Project): BazelServerService = project.getService(BazelServerService::class.java)
  }
}

class BazelServerServiceImpl(project: Project) : BazelServerService {
  override val connection: BazelServerConnection by lazy { DefaultBazelServerConnection(project) }
}
