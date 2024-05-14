package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.services.BspTaskListener
import java.util.UUID
import java.util.concurrent.CompletableFuture

public class AnalysisDebugTask(
  private val project: Project,
  private val port: Int,
  private val taskListener: BspTaskListener,
) {
  public fun connectAndExecute(targetsIds: List<BuildTargetIdentifier>): CompletableFuture<AnalysisDebugResult> {
    val originId = "analysis-debug-" + UUID.randomUUID().toString()

    BspTaskEventsService.getInstance(project).saveListener(originId, taskListener)

    val params = AnalysisDebugParams(originId, port, targetsIds)

    return doExecute(params).apply {
      handle { _, _ -> BspTaskEventsService.getInstance(project).removeListener(originId) }
    }
  }

  private fun doExecute(params: AnalysisDebugParams): CompletableFuture<AnalysisDebugResult> =
    project.connection.runWithServer { server, _ -> server.buildTargetAnalysisDebug(params) }
}
