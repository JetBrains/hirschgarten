package org.jetbrains.bazel.hotswap

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
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.pressKey
import com.intellij.tools.ide.performanceTesting.commands.reloadFiles
import com.intellij.tools.ide.performanceTesting.commands.setBreakpoint
import com.intellij.tools.ide.performanceTesting.commands.sleep
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.config.BazelHotSwapBundle
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test
import kotlin.io.path.div

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
    val commands =
      CommandChain()
        .takeScreenshot("startSync")
        .waitForBazelSync()
        .waitForSmartMode()
        .openFile("SimpleKotlinTest.kt")
        .setBreakpoint(line = 7, "SimpleKotlinTest.kt")
        .setBreakpoint(line = 11, "SimpleKotlinTest.kt")
        .debugLocalJvmSimpleKotlinTest()
        .sleep(5000)
        // bring back the focus
        .openFile("SimpleKotlinTest.kt")
        .goto(10, 35)
        .pressKey(Keys.ENTER)
        .delayType(delayMs = 150, text = "val a = 10")
        .pressKey(Keys.ENTER)
        .delayType(delayMs = 150, text = "val b = 20")
        .pressKey(Keys.ENTER)
        .delayType(delayMs = 150, text = "print(a + b)")
        .reloadFiles()
        .build(listOf("SimpleKotlinTest"))
        .sleep(5000)
        .takeScreenshot("finishBuildAction")
        .debugStep(DebugStepTypes.OVER)
        .debugStep(DebugStepTypes.OVER)
        .sleep(2000)
        .takeScreenshot("afterHotSwapDebugStep")
        .exitApp()
    val startResult = createContext().runIDE(commands = commands, runTimeout = timeout)
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
