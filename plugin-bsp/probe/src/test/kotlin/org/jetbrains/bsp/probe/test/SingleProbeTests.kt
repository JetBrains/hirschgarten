package org.jetbrains.bsp.probe.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.virtuslab.ideprobe.robot.RobotProbeDriver
import scala.Option
import scala.runtime.BoxedUnit

class SingleProbeTests {

  companion object {
    const val LATEST_VERSION = "231.8109.175"
  }

  @Test
  fun `open fresh instance of bazel-bsp project and check imported targets`() {
    with(IdeProbeTestRunner()) {
      val fixture = fixtureWithWorkspaceFromGit(
        "https://github.com/JetBrains/bazel-bsp.git",
        "2.3.0"
      ).withBuild(LATEST_VERSION)
      runAfterOpen(fixture, Option.apply(null)) { _, robot ->
        val stripeButton = robot.findElement(query.className("StripeButton", "text" to "BSP"))
        stripeButton.doClick()
        val buildPanel = robot.findElement(query.className("BspToolWindowPanel"))
        val loaded =
          buildPanel.findElement(query.className("ActionButton", "myaction.key" to "widget.loaded.targets.tab.name"))
        loaded.click()
        val targetsTree = buildPanel.findElement(query.className("Tree"))
        Assertions.assertEquals(9, targetsTree.fullTexts().size)
        robot.assertNoProblems()
        BoxedUnit.UNIT
      }
    }
  }

  @Test
  fun `open fresh instance of bazel project and check build console output for errors`() {
    with(IdeProbeTestRunner()) {
      val fixture = fixtureWithWorkspaceFromGit(
        "https://github.com/bazelbuild/bazel.git",
        "6.0.0"
      ).withBuild(LATEST_VERSION)
      runAfterOpen(fixture, Option.apply(null)) { _, robot ->
        robot.assertNoProblems()
        BoxedUnit.UNIT
      }
    }
  }

  private fun RobotProbeDriver.assertNoProblems() {
    findElement(query.className("StripeButton", "text" to "Problems")).doClick()
    val errors = findElement(query.className("ContentTabLabel", "visible_text" to "Project Errors"))
    errors.fixture().click()
    val problemsTree = findElement(query.className("ProblemsViewPanel"))
      .findElement(query.className("Tree"))
    val problemsText = problemsTree.fullText().split("\n").firstOrNull()
    Assertions.assertEquals("No errors found by the IDE", problemsText)
  }
}