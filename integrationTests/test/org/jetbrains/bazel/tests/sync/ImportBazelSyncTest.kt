package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.welcomeScreen
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.getProjectInfoFromSystemProperties
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import java.nio.file.Path
import kotlin.io.path.appendLines
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val LOCAL_HIRSCHGARTEN = "/tmp/hirschgarten"
private const val LOCAL_PLUGIN_ZIP = "/tmp/bazel-plugin-2026.1.1.zip"

private val SDKROOT: String? by lazy {
  if (System.getProperty("os.name").lowercase().contains("mac")) {
    runCatching {
      ProcessBuilder("xcrun", "--show-sdk-path")
        .redirectErrorStream(true)
        .start()
        .inputStream.bufferedReader().readText().trim()
    }.getOrNull()
  } else null
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ImportBazelSyncTest : IdeStarterBaseProjectTest() {

  override val timeout: Duration get() = 3600.seconds

  private lateinit var projectHome: Path

  private val previousPluginZip: Path?
    get() = System.getenv("BAZEL_PREVIOUS_PLUGIN_ZIP")?.let { Path.of(it) }
      ?: Path.of(LOCAL_PLUGIN_ZIP).takeIf { it.toFile().exists() }

  private fun localProjectInfo(): LocalProjectInfo = LocalProjectInfo(
    projectDir = Path.of(LOCAL_HIRSCHGARTEN),
    isReusable = true,
    configureProjectBeforeUse = ::configureLocalProject,
  )

  private var originalBazelrc: String? = null

  private fun configureLocalProject(context: IDETestContext) {
    val bazelrc = context.resolvedProjectHome.resolve(".bazelrc")
    if (originalBazelrc == null && bazelrc.toFile().exists()) {
      originalBazelrc = bazelrc.readText()
    }
    originalBazelrc?.let { bazelrc.writeText(it) }

    BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context)
    context.resolvedProjectHome.resolve("target-order-snapshot.txt").deleteIfExists()
    SDKROOT?.let { sdkroot ->
      bazelrc.appendLines(listOf("build --action_env=SDKROOT=$sdkroot"))
    }
  }

  @Test
  @Order(1)
  fun `import hirschgarten and verify target order stability`() {
    val projectInfo = if (Path.of(LOCAL_HIRSCHGARTEN).toFile().isDirectory) {
      localProjectInfo()
    } else {
      getProjectInfoFromSystemProperties()
    }
    val case = IdeaBazelCases.withProject(projectInfo)
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

    val projectInfo = LocalProjectInfo(
      projectDir = projectHome,
      isReusable = true,
      configureProjectBeforeUse = {},
    )

    // IDE Run 1: Open with OLD (previous stable) plugin
    val oldPluginContext = createContext(
      "hirschgarten-old-plugin",
      IdeaBazelCases.withProject(projectInfo),
      pluginZipOverride = previousPluginZip,
    )
    oldPluginContext
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          waitForIndicators(5.minutes)
          takeScreenshot("oldPluginOpened")
        }

        step("Check IDEA log for exceptions (old plugin)") {
          checkIdeaLogForExceptions(oldPluginContext)
        }
      }

    // IDE Run 2: Open with CURRENT (newly built) plugin — simulates upgrade
    val upgradeContext = createContext(
      "hirschgarten-upgrade",
      IdeaBazelCases.withProject(projectInfo),
    )
    upgradeContext
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
          checkIdeaLogForExceptions(upgradeContext)
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
