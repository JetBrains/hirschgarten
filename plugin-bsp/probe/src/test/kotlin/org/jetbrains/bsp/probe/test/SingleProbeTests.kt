package org.jetbrains.bsp.probe.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import scala.Option
import scala.runtime.BoxedUnit

class SingleProbeTests {

  @Test
  fun `open fresh instance of bazel-bsp project and check imported targets`() {
    with(IdeProbeTestRunner()) {
      val fixture = fixtureWithWorkspaceFromGit(
        "https://github.com/JetBrains/bazel-bsp.git",
        "2.3.0"
      ).withBuild("231.5920.14-EAP-SNAPSHOT")
      runAfterOpen(fixture, Option.apply(null)) { _, robot ->
        val stripeButton = robot.findElement(query.className("StripeButton", "text" to "BSP"))
        stripeButton.doClick()
        val buildPanel = robot.findElement(query.className("BspToolWindowPanel"))
        val loaded =
          buildPanel.findElement(query.className("ActionButton", "myaction.key" to "widget.loaded.targets.tab.name"))
        loaded.click()
        val targetsTree = buildPanel.findElement(query.className("Tree"))
        Assertions.assertEquals(9, targetsTree.fullTexts().size)
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
      ).withBuild("231.5920.14-EAP-SNAPSHOT")
      runAfterOpen(fixture, Option.apply(null)) { _, robot ->
        val numberOfErrorsInBuildOutput = robot.getBuildConsoleOutput().filter { it.startsWith("ERROR: ") }.size
        Assertions.assertEquals(0, numberOfErrorsInBuildOutput)
        BoxedUnit.UNIT
      }
    }
  }
}