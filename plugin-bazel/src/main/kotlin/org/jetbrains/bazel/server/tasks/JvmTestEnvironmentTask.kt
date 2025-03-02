package org.jetbrains.bazel.server.tasks

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult

class JvmTestEnvironmentTask(project: Project) : BspServerSingleTargetTask<JvmTestEnvironmentResult>("jvmTestEnvironment", project) {
  override suspend fun executeWithServer(server: JoinedBuildServer, targetId: Label): JvmTestEnvironmentResult {
    val params = createJvmTestEnvironmentParams(targetId)
    return server.buildTargetJvmTestEnvironment(params)
  }

  private fun createJvmTestEnvironmentParams(targetId: Label) = JvmTestEnvironmentParams(listOf(targetId))
}
