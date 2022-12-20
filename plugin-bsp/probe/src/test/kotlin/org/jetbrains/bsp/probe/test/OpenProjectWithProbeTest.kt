package org.jetbrains.bsp.probe.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import scala.runtime.BoxedUnit

class OpenProjectWithProbeTest {

  @Test
  fun `open fresh instance of bazel-bsp project and check imported targets`() {
    with(IdeProbeTestRunner()) {
      val fixture = fixtureWithWorkspaceFromGit(
        "https://github.com/JetBrains/bazel-bsp.git",
        "2.3.0"
      ).withBuild("223.8214.27")
      runAfterOpen(fixture) { _, robot ->
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
}