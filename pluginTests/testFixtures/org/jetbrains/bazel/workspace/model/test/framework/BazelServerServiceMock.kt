package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.server.BazelServerConnection
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bsp.protocol.TaskId

class MockBuildServerService(server: BazelServerFacade) : BazelServerService {
  override val connection: BazelServerConnection =
    object : BazelServerConnection {
      override suspend fun <T> runWithServer(taskId: TaskId?, task: suspend (server: BazelServerFacade) -> T): T = task(server)
    }
}
