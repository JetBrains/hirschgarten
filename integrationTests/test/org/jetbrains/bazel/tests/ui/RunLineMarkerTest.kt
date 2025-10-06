package org.jetbrains.bazel.tests.ui

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.welcomeScreen
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class RunLineMarkerTest : IdeStarterBaseProjectTest() {

  @Test
  fun openProject() {
    val fileName = "SimpleKotlinTest.kt"
    createContext("runLineMarker", IdeaBazelCases.RunLineMarker).runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
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
      syncBazelProject()
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
