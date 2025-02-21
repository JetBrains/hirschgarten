package org.jetbrains.bazel.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.JoinedBuildServer

class JvmTestEnvironmentTask(project: Project) : BspServerSingleTargetTask<JvmTestEnvironmentResult>("jvmTestEnvironment", project) {
  override suspend fun executeWithServer(
    server: JoinedBuildServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): JvmTestEnvironmentResult {
    val params = createJvmTestEnvironmentParams(targetId)
    return server.buildTargetJvmTestEnvironment(params).get()
  }

  private fun createJvmTestEnvironmentParams(targetId: BuildTargetIdentifier) = JvmTestEnvironmentParams(listOf(targetId))
}
