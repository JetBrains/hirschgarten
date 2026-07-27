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
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.buildAndSync
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

private val RULES_PROTO_PROJECT = simpleBazelProject(
  revision = "5cbca8140ac85e5178ca803935fbd9e1d9400a07",
  path = "legacyRulesProto",
  configureProject = { context ->
    BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context)
  },
)

class RulesProtoTest : IdeStarterBaseProjectTest() {
  override val timeout = 30.minutes

  @Test
  fun `rules_proto should sync correctly`() {
    createContext("protobufResolve", IdeaBazelCases.withProject(RULES_PROTO_PROJECT))
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
