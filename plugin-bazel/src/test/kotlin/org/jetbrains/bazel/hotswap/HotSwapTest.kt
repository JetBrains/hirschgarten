package org.jetbrains.bazel.hotswap

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.metrics.collector.telemetry.OpentelemetrySpanJsonParser
import com.intellij.tools.ide.metrics.collector.telemetry.SpanFilter
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.DebugStepTypes
import com.intellij.tools.ide.performanceTesting.commands.Keys
import com.intellij.tools.ide.performanceTesting.commands.build
import com.intellij.tools.ide.performanceTesting.commands.debugStep
import com.intellij.tools.ide.performanceTesting.commands.delayType
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.pressKey
import com.intellij.tools.ide.performanceTesting.commands.reloadFiles
import com.intellij.tools.ide.performanceTesting.commands.setBreakpoint
import com.intellij.tools.ide.performanceTesting.commands.sleep
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.jetbrains.bazel.config.BazelHotSwapBundle
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.io.path.div
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/hotswap --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class HotSwapTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c88d3cb84fae0c8a42cd8ddd78306a40595ff764",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("simpleKotlinTest") },
        isReusable = false,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )

  @Test
  fun openBazelProject() {
    val startResult =
      createContext()
        .runIdeWithDriver(runTimeout = timeout)
        .useDriverAndCloseIde {
          ideFrame {
            syncBazelProject()
            waitForIndicators(5.minutes)

            step("Set breakpoints and start debug") {
              execute { openFile("SimpleKotlinTest.kt") }
              execute { setBreakpoint(line = 7, "SimpleKotlinTest.kt") }
              execute { setBreakpoint(line = 11, "SimpleKotlinTest.kt") }
              execute { debugLocalJvmSimpleKotlinTest() }
              execute { sleep(5000) }
              takeScreenshot("afterSetBreakpointsAndStartDebugSession")
            }

            step("Modify code during debug session") {
              // bring back the focus
              execute { openFile("SimpleKotlinTest.kt") }
              execute { goto(10, 35) }
              execute { pressKey(Keys.ENTER) }
              execute { delayType(delayMs = 150, text = "val a = 10") }
              execute { pressKey(Keys.ENTER) }
              execute { delayType(delayMs = 150, text = "val b = 20") }
              execute { pressKey(Keys.ENTER) }
              execute { delayType(delayMs = 150, text = "print(a + b)") }
              takeScreenshot("afterModifyCodeDuringDebugSession")
            }

            step("Apply hotswap and continue debugging") {
              execute { reloadFiles() }
              execute { build(listOf("SimpleKotlinTest")) }
              execute { sleep(5000) }
              execute { takeScreenshot("finishBuildAction") }
              execute { debugStep(DebugStepTypes.OVER) }
              execute { debugStep(DebugStepTypes.OVER) }
              execute { sleep(2000) }
              execute { takeScreenshot("afterHotSwapDebugStep") }
            }
          }
        }

    val notificationSpanElements =
      OpentelemetrySpanJsonParser(SpanFilter.nameEquals("show notification")).getSpanElements(
        startResult.runContext.logsDir / "opentelemetry.json",
      )
    var hotSwapSuccess = false
    notificationSpanElements.forEach {
      it.tags.forEach { tagsPair ->
        if (tagsPair.second.contains(BazelHotSwapBundle.message("hotswap.message.reload.success", 1))) {
          hotSwapSuccess = true
        }
      }
    }
    assert(hotSwapSuccess) { "Cannot find hotswap success message" }
  }
}

private fun <T : CommandChain> T.debugLocalJvmSimpleKotlinTest(): T {
  addCommand(CMD_PREFIX + "debugLocalJvmSimpleKotlinTest")
  return this
}
