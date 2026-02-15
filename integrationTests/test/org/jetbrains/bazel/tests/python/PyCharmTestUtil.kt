package org.jetbrains.bazel.tests.python

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.xQuery

internal fun Driver.verifyRunLineMarkerText(expectedTexts: List<String>) {
  ideFrame {
    val gutterIcons = editorTabs().gutter().getGutterIcons()
    val selectedGutterIcon = gutterIcons.first()
    selectedGutterIcon.click()
    val heavyWeightWindow = popup(xQuery { byClass("HeavyWeightWindow") })
    takeScreenshot("afterClickingOnRunLineMarker")
    val texts = heavyWeightWindow.getAllTexts()
    assert(texts.size == expectedTexts.size)
    expectedTexts.forEach { expected -> assert(texts.any { actual -> actual.text.contains(expected) }) }
  }
}
