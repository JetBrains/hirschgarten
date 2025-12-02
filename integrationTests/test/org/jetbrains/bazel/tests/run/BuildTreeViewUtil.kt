package org.jetbrains.bazel.tests.run

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.UiText
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.elements.JTreeUiComponent
import com.intellij.driver.sdk.ui.components.elements.tree
import com.intellij.driver.sdk.ui.xQuery
import org.junit.jupiter.api.Assertions

fun IdeaFrameUI.verifyBuildResultsTree(expectedTexts: Set<String>) {
  step("Verify build results tree") {
    val treeComponent = buildTreeView()
    treeComponent.expandAll()
    val actualTree = treeComponent.getAllTexts().map { it.text }
    require(expectedTexts.isNotEmpty())
    for (text in expectedTexts) {
      Assertions.assertTrue(actualTree.contains(text), "Build tree must contain $text, actual tree: $actualTree")
    }
  }
}

fun IdeaFrameUI.buildTreeView(): JTreeUiComponent =
  tree(xQuery { byAccessibleName("Build results") })

private fun List<UiText>.filterRelevant() =
  this
    .map { it.text }
//.filter { " sec" !in it && " ms" !in it && it != "Test Results" }
