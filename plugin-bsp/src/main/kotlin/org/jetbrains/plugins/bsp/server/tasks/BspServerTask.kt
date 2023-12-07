package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.connection

public abstract class BspServerTask<T>(private val taskName: String, protected val project: Project) {
  protected fun connectAndExecuteWithServer(task: (BspServer, BazelBuildServerCapabilities) -> T?): T? =
    project.connection.runWithServer(task)
}

public abstract class BspServerSingleTargetTask<T>(taskName: String, project: Project) :
  BspServerTask<T>(taskName, project) {
  public fun connectAndExecute(targetId: BuildTargetIdentifier): T? =
    connectAndExecuteWithServer { server, capabilities -> executeWithServer(server, capabilities, targetId) }

  protected abstract fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): T
}

public abstract class BspServerMultipleTargetsTask<T>(taskName: String, project: Project) :
  BspServerSingleTargetTask<T>(taskName, project) {
  protected override fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): T = executeWithServer(server, capabilities, listOf(targetId))

  public fun connectAndExecute(targetsIds: List<BuildTargetIdentifier>): T? =
    connectAndExecuteWithServer { server, capabilities -> executeWithServer(server, capabilities, targetsIds) }

  protected abstract fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetsIds: List<BuildTargetIdentifier>,
  ): T
}
