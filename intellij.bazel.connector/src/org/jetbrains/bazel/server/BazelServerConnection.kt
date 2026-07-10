package org.jetbrains.bazel.server

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.TaskId

/**
 * The BSP connection, implementation should keep all the information
 * needed to establish and keep the connection with the server.
 */
@ApiStatus.Internal
interface BazelServerConnection {
  suspend fun <T> runWithServer(taskId: TaskId? = null, task: suspend (server: BazelServerFacade) -> T): T
}

val Project.connection: BazelServerConnection
  @ApiStatus.Internal
  get() = BazelServerService.getInstance(this).connection

@ApiStatus.Internal
interface BazelServerService {
  val connection: BazelServerConnection

  companion object {
    fun getInstance(project: Project): BazelServerService = project.getService(BazelServerService::class.java)
  }
}
