package org.jetbrains.bsp.probe.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.WaitLogic
import org.virtuslab.ideprobe.robot.RobotProbeDriver
import org.virtuslab.ideprobe.robot.RobotSyntax.SearchableOps
import scala.Option
import scala.runtime.BoxedUnit

class SingleProbeTests {

  @Test
  fun `open fresh instance of bazel-bsp project and check imported targets`() {
    with(IdeProbeTestRunner()) {
      val fixture = fixtureWithWorkspaceFromGit(
        "https://github.com/JetBrains/bazel-bsp.git",
        "3.0.0",
      )
      runAfterOpen(fixture, Option.apply(null)) { probe, robot, intellij ->
        fun testTargetsTree(buildPanel: SearchableOps) {
          val loaded =
            buildPanel.findElement(Query.className("ActionButton", "myaction.key" to "widget.loaded.targets.tab.name"))
          loaded.click()
          val targetsTree = buildPanel.findElement(Query.className("Tree"))
          Assertions.assertEquals(9, targetsTree.fullTexts().size)
        }

        val stripeButton = robot.findElement(Query.className("StripeButton", "text" to "Bazel"))
        stripeButton.doClick()
        val buildPanel = robot.findElement(Query.className("BspToolWindowPanel"))
        testTargetsTree(buildPanel)

        robot.assertNoProblems(probe)
        closeProject(probe)
        robot.reopenProject(probe, intellij)

        val newBuildPanel = robot.findElement(Query.className("BspToolWindowPanel"))
        val reconnect =
          newBuildPanel.findElement(Query.className("ActionButton", "myaction.key" to "connect.action.text"))
        reconnect.click()
        probe.await(emptyBackgroundTaskWithoutTimeouts {
          robot.findElement(Query.className("StripeButton", "text" to "Bazel"))
        })
        testTargetsTree(newBuildPanel)
        BoxedUnit.UNIT
      }
    }
  }

  @Test
  fun `open fresh instance of bazel project and check build console output for errors`() {
    with(IdeProbeTestRunner()) {
      val fixture = fixtureWithWorkspaceFromGit(
        "https://github.com/bazelbuild/bazel.git", "6.0.0",
      )
      runAfterOpen(fixture, Option.apply(null)) { probe, robot, _ ->
        robot.assertNoProblems(probe)

        BoxedUnit.UNIT
      }
    }
  }

  private fun RobotProbeDriver.reopenProject(probe: ProbeDriver, intellij: RunningIntelliJFixture) {
    probe.tryUntilSuccessful {
      probe.openProject(intellij.workspace(), WaitLogic.Default())
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
