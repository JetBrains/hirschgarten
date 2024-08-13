package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.JoinedBuildServer

public class JvmRunEnvironmentTask(project: Project) : BspServerSingleTargetTask<JvmRunEnvironmentResult>("jvmRunEnvironment", project) {
  override fun executeWithServer(
    server: JoinedBuildServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): JvmRunEnvironmentResult {
    val params = createJvmRunEnvironmentParams(targetId)
    return server.buildTargetJvmRunEnvironment(params).get()
  }

  private fun createJvmRunEnvironmentParams(targetId: BuildTargetIdentifier) = JvmRunEnvironmentParams(listOf(targetId))
}
