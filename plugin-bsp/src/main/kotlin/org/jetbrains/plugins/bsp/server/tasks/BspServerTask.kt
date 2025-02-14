package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.server.connection.connection

abstract class BspServerTask<T>(private val taskName: String, protected val project: Project) {
  protected suspend fun connectAndExecuteWithServer(task: suspend (JoinedBuildServer, BazelBuildServerCapabilities) -> T?): T? =
    project.connection.runWithServer(task)
}

abstract class BspServerSingleTargetTask<T>(taskName: String, project: Project) : BspServerTask<T>(taskName, project) {
  suspend fun connectAndExecute(targetId: BuildTargetIdentifier): T? =
    connectAndExecuteWithServer { server, capabilities -> executeWithServer(server, capabilities, targetId) }

  protected abstract suspend fun executeWithServer(
    server: JoinedBuildServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): T
}

abstract class BspServerMultipleTargetsTask<T>(taskName: String, project: Project) : BspServerSingleTargetTask<T>(taskName, project) {
  override suspend fun executeWithServer(
    server: JoinedBuildServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): T = executeWithServer(server, capabilities, listOf(targetId))

  suspend fun connectAndExecute(targetsIds: List<BuildTargetIdentifier>): T? =
    connectAndExecuteWithServer { server, capabilities -> executeWithServer(server, capabilities, targetsIds) }

  protected abstract suspend fun executeWithServer(
    server: JoinedBuildServer,
    capabilities: BuildServerCapabilities,
    targetsIds: List<BuildTargetIdentifier>,
  ): T
}
