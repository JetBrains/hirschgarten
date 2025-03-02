package org.jetbrains.bazel.server.tasks

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult

class JvmRunEnvironmentTask(project: Project) : BspServerSingleTargetTask<JvmRunEnvironmentResult>("jvmRunEnvironment", project) {
  override suspend fun executeWithServer(server: JoinedBuildServer, targetId: Label): JvmRunEnvironmentResult {
    val params = createJvmRunEnvironmentParams(targetId)
    return server.buildTargetJvmRunEnvironment(params)
  }

  private fun createJvmRunEnvironmentParams(targetId: Label) = JvmRunEnvironmentParams(listOf(targetId))
}
