package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import com.intellij.openapi.project.Project
import java.util.*

public class RunTargetTask(project: Project) : BspServerSingleTargetTask<RunResult>(project) {

  public override fun execute(targetId: BuildTargetIdentifier): RunResult {
    val runParams = createRunParams(targetId)

    return server.buildTargetRun(runParams).get()
  }

  private fun createRunParams(targetId: BuildTargetIdentifier): RunParams =
    RunParams(targetId).apply {
      // TODO
      originId = "run-" + UUID.randomUUID().toString()
      arguments = listOf()
    }
}
