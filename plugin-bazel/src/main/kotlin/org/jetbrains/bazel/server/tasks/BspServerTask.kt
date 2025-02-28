package org.jetbrains.bazel.server.tasks

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.JoinedBuildServer

abstract class BspServerTask<T>(private val taskName: String, protected val project: Project) {
  protected suspend fun connectAndExecuteWithServer(task: suspend (JoinedBuildServer) -> T?): T? = project.connection.runWithServer(task)
}

abstract class BspServerSingleTargetTask<T>(taskName: String, project: Project) : BspServerTask<T>(taskName, project) {
  suspend fun connectAndExecute(targetId: BuildTargetIdentifier): T? =
    connectAndExecuteWithServer { server -> executeWithServer(server, targetId) }

  protected abstract suspend fun executeWithServer(server: JoinedBuildServer, targetId: BuildTargetIdentifier): T
}

abstract class BspServerMultipleTargetsTask<T>(taskName: String, project: Project) : BspServerSingleTargetTask<T>(taskName, project) {
  override suspend fun executeWithServer(server: JoinedBuildServer, targetId: BuildTargetIdentifier): T =
    executeWithServer(server, listOf(targetId))

  suspend fun connectAndExecute(targetsIds: List<BuildTargetIdentifier>): T? =
    connectAndExecuteWithServer { server -> executeWithServer(server, targetsIds) }

  protected abstract suspend fun executeWithServer(server: JoinedBuildServer, targetsIds: List<BuildTargetIdentifier>): T
}
