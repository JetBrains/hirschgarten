package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestResult
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.BspServer
import java.util.*

public class TestTargetTask(project: Project) : BspServerSingleTargetTask<TestResult>("test target", project) {
  protected override fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): TestResult {
    val params = createTestParams(targetId)

    return server.buildTargetTest(params).get()
  }

  private fun createTestParams(targetId: BuildTargetIdentifier): TestParams =
    TestParams(listOf(targetId)).apply {
      originId = "test-" + UUID.randomUUID().toString()
      arguments = emptyList()
    }
}
