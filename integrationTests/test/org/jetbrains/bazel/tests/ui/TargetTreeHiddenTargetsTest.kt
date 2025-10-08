package org.jetbrains.bazel.tests.ui

import com.intellij.driver.client.Remote
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.remote.Component
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TargetTreeHiddenTargetsTest : IdeStarterBaseProjectTest() {

  @Test
  fun testHiddenTargets() {
    createContext("targetTreeHiddenTargetsTest", IdeaBazelCases.TestHiddenTargetsTest)
      .applyVMOptionsPatch {
        addSystemProperty("expose.ui.hierarchy.url", "true")
      }
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          step("Sync project") {
            syncBazelProject()
            waitForIndicators(5.minutes)
            takeScreenshot("afterSync")
          }

          step("Show hidden targets") {
            val tree = x("//div[@class='BuildTargetTree']")

            fun hasKotest() = tree.getAllTexts { true }
              .any { it.text.contains("io_kotest_kotest_assertions_core_jvm") }

            assert(!hasKotest()) { "Kotest is present in the tree" }

            x { byAccessibleName("Show Hidden targets") }.click()

            wait(5.seconds)

            driver.utility(TreeUtil::class)
              .expandAll(tree.component)

            assert(hasKotest()) { "Kotest is not present in the tree" }
          }
        }
      }
  }
}

@Remote("com.intellij.util.ui.tree.TreeUtil")
interface TreeUtil {
  fun expandAll(tree: Component)
}
