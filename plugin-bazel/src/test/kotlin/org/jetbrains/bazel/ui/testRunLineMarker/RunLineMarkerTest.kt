package org.jetbrains.bazel.ui.testRunLineMarker

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.openFile
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.welcomeScreen
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class RunLineMarkerTest : IdeStarterBaseProjectTest() {
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
  fun openProject() {
    val fileName = "SimpleKotlinTest.kt"
    val commands =
      CommandChain()
        .takeScreenshot("startSync")
        .waitForBazelSync()
        .waitForSmartMode()
    createContext().runIdeWithDriver(runTimeout = timeout, commands = commands).useDriverAndCloseIde {
      verifyRunLineMarkerText(fileName)
      takeScreenshot("afterClickingOnRunLineMarker1")
      invokeAction("CloseProject")
      // simulating reopening project
      welcomeScreen { clickRecentProject("simpleKotlinTest") }
      verifyRunLineMarkerText(fileName)
      takeScreenshot("afterClickingOnRunLineMarker2")
    }
  }

  private fun Driver.verifyRunLineMarkerText(fileName: String) {
    ideFrame {
      waitForIndicators(5.minutes)
      openFile(fileName)
      val gutterIcons = editorTabs().gutter().getGutterIcons()
      val selectedGutterIcon = gutterIcons.first()
      selectedGutterIcon.click()
      val heavyWeightWindow = popup(xQuery { byClass("HeavyWeightWindow") })
      val texts = heavyWeightWindow.getAllTexts()
      assert(texts.any { it.text.contains("Test") })
    }
  }
}
