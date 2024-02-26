package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BazelBuildServer
import org.jetbrains.bsp.protocol.RemoteDebugData
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.ui.configuration.run.BspDebugType
import java.util.UUID

public class RunTargetTask(
  project: Project,
  private val debugType: BspDebugType?,
  private val port: Int?,
) : BspServerSingleTargetTask<RunResult>("run target", project) {
  protected override fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): RunResult {
    saveAllFiles()
    return if (debugType == null || port == null) {
      val runParams = createRunParams(targetId)
      server.buildTargetRun(runParams).get()
    } else {
      val debugParams = createDebugParams(targetId, debugType, port)
      (server as BazelBuildServer).buildTargetRunWithDebug(debugParams).get()
    }
  }

  private fun createRunParams(targetId: BuildTargetIdentifier): RunParams =
    RunParams(targetId).apply {
      originId = "run-" + UUID.randomUUID().toString()
      arguments = listOf()
    }

  private fun createDebugParams(
    targetId: BuildTargetIdentifier,
    type: BspDebugType,
    port: Int,
  ): RunWithDebugParams {
    val runParams = createRunParams(targetId)
    return RunWithDebugParams(
      originId = runParams.originId,
      runParams = runParams,
      debug = RemoteDebugData(type.s, port)
    )
  }
}
