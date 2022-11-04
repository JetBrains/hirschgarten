package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestResult
import com.intellij.openapi.project.Project
import java.util.*

public class TestTargetTask(project: Project) : BspServerSingleTargetTask<TestResult>(project) {

  public override fun execute(targetId: BuildTargetIdentifier): TestResult {
    val params = createTestParams(targetId)

    return server.buildTargetTest(params).get()
  }

  private fun createTestParams(targetId: BuildTargetIdentifier): TestParams =
    TestParams(listOf(targetId)).apply {
      // TODO
      originId = "test-" + UUID.randomUUID().toString()
      arguments = emptyList()
    }
}
