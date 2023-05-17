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

  companion object {
    const val LATEST_NIGHTLY_VERSION = "232.5150.116"
    val LATEST_VERSION : String? = null
  }

  @Test
  fun `open fresh instance of bazel-bsp project and check imported targets`() {
    with(IdeProbeTestRunner()) {
      val fixture = fixtureWithWorkspaceFromGit(
        "https://github.com/JetBrains/bazel-bsp.git",
        "2.3.0"
      ).withBuild(LATEST_NIGHTLY_VERSION, LATEST_VERSION)
      runAfterOpen(fixture, Option.apply(null)) { probe, robot, intellij ->
        fun testTargetsTree(buildPanel: SearchableOps) {
          val loaded =
            buildPanel.findElement(query.className("ActionButton", "myaction.key" to "widget.loaded.targets.tab.name"))
          loaded.click()
          val targetsTree = buildPanel.findElement(query.className("Tree"))
          Assertions.assertEquals(9, targetsTree.fullTexts().size)
        }

        val stripeButton = robot.findElement(query.className("StripeButton", "text" to "BSP"))
        stripeButton.doClick()
        val buildPanel = robot.findElement(query.className("BspToolWindowPanel"))
        testTargetsTree(buildPanel)

        robot.assertNoProblems(probe)
        robot.reopenProject(probe, intellij)

        val newBuildPanel = robot.findElement(query.className("BspToolWindowPanel"))
        val reconnect =
          newBuildPanel.findElement(query.className("ActionButton", "myaction.key" to "connect.action.text"))
        reconnect.click()
        probe.await(emptyBackgroundTaskWithoutTimeouts {
          robot.findElement(query.className("StripeButton", "text" to "BSP"))
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
        "https://github.com/bazelbuild/bazel.git", "6.0.0"
      ).withBuild(LATEST_NIGHTLY_VERSION, LATEST_VERSION)
      runAfterOpen(fixture, Option.apply(null)) { probe, robot, _ ->
        robot.assertNoProblems(probe)
        BoxedUnit.UNIT
      }
    }
  }

  private fun RobotProbeDriver.reopenProject(probe: ProbeDriver, intellij: RunningIntelliJFixture) {
    val file = findElement(
      query.className(
        "ActionMenu",
        "text.key" to "group.FileMenu.text action.NewFile.text problems.view.highlighting"
      )
    )
    file.doClick()
    probe.tryUntilSuccessful {
      val closeProject = file.find(query.div("visible_text" to "Close Project"))
      closeProject.click()
    }
    probe.tryUntilSuccessful {
      probe.openProject(intellij.workspace(), WaitLogic.Default())
    }
  }

  private fun RobotProbeDriver.assertNoProblems(probe: ProbeDriver) {
    findElement(query.className("StripeButton", "text" to "Problems")).doClick()
    val errors = findElement(query.className("ContentTabLabel", "visible_text" to "Project Errors"))
    var problemsText = ""
    probe.tryUntilSuccessful {
      errors.fixture().click()
      val problemsTree = findElement(query.className("ProblemsViewPanel"))
        .findElement(query.className("Tree"))
      problemsText = problemsTree.fullText().split("\n").firstOrNull().orEmpty()
    }
    Assertions.assertEquals("No errors found by the IDE", problemsText)
  }
}