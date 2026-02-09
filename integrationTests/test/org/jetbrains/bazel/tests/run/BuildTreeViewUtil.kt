package org.jetbrains.bazel.tests.run

import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.elements.JTreeUiComponent
import com.intellij.driver.sdk.ui.components.elements.tree
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.waitFor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun IdeaFrameUI.waitForBuildResultsTree(expectedTexts: Set<String>, timeout: Duration = 1.minutes) {
  require(expectedTexts.isNotEmpty())
  waitFor(
    message = "Build results tree to contain $expectedTexts",
    timeout = timeout,
    interval = 2.seconds,
  ) {
    val treeComponent = buildTreeView()
    treeComponent.expandAll()
    val actualTree = treeComponent.getAllTexts().map { it.text }
    expectedTexts.all { it in actualTree }
  }
}

fun IdeaFrameUI.buildTreeView(): JTreeUiComponent =
  tree(xQuery { byAccessibleName("Build results") })
