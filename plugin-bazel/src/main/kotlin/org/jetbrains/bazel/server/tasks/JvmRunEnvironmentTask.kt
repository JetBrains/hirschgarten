package org.jetbrains.bazel.server.tasks

import com.intellij.openapi.project.Project
import kotlinx.coroutines.future.await
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult

class JvmRunEnvironmentTask(project: Project) : BspServerSingleTargetTask<JvmRunEnvironmentResult>("jvmRunEnvironment", project) {
  override suspend fun executeWithServer(server: JoinedBuildServer, targetId: BuildTargetIdentifier): JvmRunEnvironmentResult {
    val params = createJvmRunEnvironmentParams(targetId)
    return server.buildTargetJvmRunEnvironment(params).await()
  }

  private fun createJvmRunEnvironmentParams(targetId: BuildTargetIdentifier) = JvmRunEnvironmentParams(listOf(targetId))
}
