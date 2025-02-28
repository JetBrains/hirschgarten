package org.jetbrains.bazel.server.tasks

import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult

class JvmTestEnvironmentTask(project: Project) : BspServerSingleTargetTask<JvmTestEnvironmentResult>("jvmTestEnvironment", project) {
  override suspend fun executeWithServer(server: JoinedBuildServer, targetId: BuildTargetIdentifier): JvmTestEnvironmentResult {
    val params = createJvmTestEnvironmentParams(targetId)
    return server.buildTargetJvmTestEnvironment(params)
  }

  private fun createJvmTestEnvironmentParams(targetId: BuildTargetIdentifier) = JvmTestEnvironmentParams(listOf(targetId))
}
