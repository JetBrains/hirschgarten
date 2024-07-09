package org.jetbrains.bsp.probe.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.robot.RobotProbeDriver
import org.virtuslab.ideprobe.robot.RobotSyntax.SearchableOps
import scala.runtime.BoxedUnit
import java.nio.file.Paths


class LocalProbeTests {

  @Test
  fun `open local instance of bazel-bsp project and check imported targets`() {
    val localBazelBspPath = Paths.get("/tmp/bazel-bsp")

    with(
      IdeProbeTestRunner(
        localBazelBspPath
      )
    ) {
      runIntellijAndOpenProject { probe, robot, intellij ->
        fun testTargetsTree(buildPanel: SearchableOps) {
          val targetsTree = buildPanel.findElement(Query.className("Tree"))
          probe.screenshot("_bazel-bsp-build-panel")
          Assertions.assertEquals(10, targetsTree.fullTexts().size)
        }

        probe.screenshot("_bazel-bsp-on-open")
        val stripeButton = robot.findElement(Query.className("StripeButton", "text" to "Bazel"))
        probe.screenshot("_bazel-bsp-button-before-click")
        stripeButton.doClick()
        val buildPanel = robot.findElement(Query.className("BspToolWindowPanel"))
        probe.screenshot("_bazel-bsp-button-after-click")
        testTargetsTree(buildPanel)
        robot.assertNoProblems(probe)
        probe.screenshot("_bazel-bsp-project-before-closing")
        BoxedUnit.UNIT
      }
    }
  }

  private fun RobotProbeDriver.assertNoProblems(probe: ProbeDriver) {
    findElement(Query.className("StripeButton", "text" to "Problems")).doClick()
    val errors = findElement(Query.className("ContentTabLabel", "visible_text" to "Project Errors"))
    var problemsText = ""
    probe.tryUntilSuccessful {
      errors.fixture().click()
      val problemsTree = findElement(Query.className("ProblemsViewPanel"))
        .findElement(Query.className("Tree"))
      problemsText = problemsTree.fullText().split("\n").firstOrNull().orEmpty()
    }
    Assertions.assertEquals("No errors found by the IDE", problemsText)
  }
}
