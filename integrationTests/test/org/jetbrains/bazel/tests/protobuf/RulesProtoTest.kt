package org.jetbrains.bazel.tests.protobuf

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.assertCurrentFile
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.goToDeclaration
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.buildAndSync
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class RulesProtoTest : IdeStarterBaseProjectTest() {
  override val timeout = 30.minutes

  @Test
  fun `rules_proto should sync correctly`() {
    createContext("protobufResolve", IdeaBazelCases.RulesProtoTest)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          execute { buildAndSync() }
          waitForIndicators(10.minutes)

          step("Sanity check consumer") {
            execute { openFile("consumerJava/Main.java") }
            takeScreenshot("afterOpenMain")
            execute { checkOnRedCode() }
          }

          step("Check basic navigation") {
            execute { openFile("libB/lib_b.proto") }
            takeScreenshot("afterOpenLibB")
            execute { checkOnRedCode() }
            execute { goto(10, 11)}
            takeScreenshot("beforeNavigation")
            execute { goToDeclaration() }
            takeScreenshot("afterNavigation")
            execute { assertCurrentFile("lib_a.proto") }
          }
        }
      }
  }

}
