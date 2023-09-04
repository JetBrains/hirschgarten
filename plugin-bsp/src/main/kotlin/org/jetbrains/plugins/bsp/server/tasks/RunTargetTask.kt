package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.BspServer
import java.util.*

public class RunTargetTask(project: Project) : BspServerSingleTargetTask<RunResult>("run target", project) {
  protected override fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): RunResult {
    saveAllFiles()
    val runParams = createRunParams(targetId)

    return server.buildTargetRun(runParams).get()
  }

  private fun createRunParams(targetId: BuildTargetIdentifier): RunParams =
    RunParams(targetId).apply {
      originId = "run-" + UUID.randomUUID().toString()
      arguments = listOf()
    }
}
