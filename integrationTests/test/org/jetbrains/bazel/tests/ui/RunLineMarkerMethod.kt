package org.jetbrains.bazel.tests.ui

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.welcomeScreen
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.ide.starter.driver.engine.BackgroundRun
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.syncBazelProject
import kotlin.time.Duration.Companion.minutes

fun runLineMarkerPersistence(bgRun: BackgroundRun) {
  val fileName = "SimpleKotlinTest.kt"
  bgRun.driver.withContext {
    verifyKotlinRunLineMarkerText(fileName)
    takeScreenshot("afterClickingOnRunLineMarker1")
    invokeAction("CloseProject")
    welcomeScreen { clickRecentProject("simpleKotlinTest") }
    verifyKotlinRunLineMarkerText(fileName)
    takeScreenshot("afterClickingOnRunLineMarker2")
  }
}

private fun Driver.verifyKotlinRunLineMarkerText(fileName: String) {
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
