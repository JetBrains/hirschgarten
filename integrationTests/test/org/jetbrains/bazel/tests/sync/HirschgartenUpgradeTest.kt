package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.welcomeScreen
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.getProjectInfoFromSystemProperties
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.test.compat.IntegrationTestCompat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class HirschgartenUpgradeTest : IdeStarterBaseProjectTest() {

  private lateinit var projectHome: Path

  private val previousPluginZip: Path?
    get() = System.getenv("BAZEL_PREVIOUS_PLUGIN_ZIP")?.let { Path.of(it) }

  @Test
  @Order(1)
  fun `import hirschgarten and verify target order stability`() {
    val case = IdeaBazelCases.withProject(getProjectInfoFromSystemProperties())
    val context = createContext("hirschgarten", case)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(10.minutes)
          assertSyncSucceeded()

          step("Save target order after initial import") {
            execute { saveTargetOrder() }
            takeScreenshot("afterSaveTargetOrder")
          }
        }

        step("Close project") {
          invokeAction("CloseProject")
          takeScreenshot("afterCloseProject")
        }

        step("Reopen project from welcome screen") {
          welcomeScreen { clickRecentProject("hirschgarten") }
          takeScreenshot("afterReopen")
        }

        ideFrame {
          waitForIndicators(5.minutes)

          step("Assert target order unchanged after reopen") {
            execute { assertTargetOrder() }
            takeScreenshot("afterAssertTargetOrder")
          }
        }

        projectHome = context.resolvedProjectHome

        step("Check IDEA log for exceptions") {
          checkIdeaLogForExceptions(context)
        }
      }
  }

  @Test
  @Order(2)
  fun `plugin upgrade preserves project state`() {
    check(::projectHome.isInitialized) { "Test 1 must run first to populate projectHome" }
    val pluginZip = checkNotNull(previousPluginZip) { "BAZEL_PREVIOUS_PLUGIN_ZIP env var must be set" }

    val context = createContext(
      "hirschgarten-upgrade",
      IdeaBazelCases.withProject(
        LocalProjectInfo(
          projectDir = projectHome,
          isReusable = true,
          configureProjectBeforeUse = {},
        ),
      ),
      pluginZipOverride = pluginZip,
    )

    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          waitForIndicators(5.minutes)
          takeScreenshot("oldPluginOpened")
        }

        step("Check IDEA log for exceptions (old plugin)") {
          checkIdeaLogForExceptions(context)
        }
      }

    context.paths.pluginsDir.toFile().listFiles()
      ?.filter { it.name.contains("bazel", ignoreCase = true) }
      ?.forEach { it.deleteRecursively() }
    IntegrationTestCompat.onPostCreateContext(context)

    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Verify no resync happens after plugin upgrade") {
          ideFrame {
            wait(20.seconds)
            try {
              val buildView = x { byType("com.intellij.build.BuildView") }
              assert(
                !buildView.getAllTexts().any {
                  it.text.contains(BazelPluginBundle.message("console.task.sync.in.progress"))
                },
              ) { "Unexpected resync triggered after plugin upgrade" }
            } catch (e: Exception) {
              assert(e is WaitForException) { "Unknown exception: ${e.message}" }
            }
          }
          takeScreenshot("afterNoResyncCheck")
        }

        ideFrame {
          step("Assert target order matches after upgrade") {
            execute { assertTargetOrder() }
            takeScreenshot("afterUpgradeAssertTargetOrder")
          }
        }

        step("Check IDEA log for exceptions (upgraded plugin)") {
          checkIdeaLogForExceptions(context)
        }
      }
  }
}

private fun <T : CommandChain> T.saveTargetOrder(): T {
  addCommand(CMD_PREFIX + "saveTargetOrder")
  return this
}

private fun <T : CommandChain> T.assertTargetOrder(): T {
  addCommand(CMD_PREFIX + "assertTargetOrder")
  return this
}
