package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.UiText
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.elements.JTreeUiComponent
import com.intellij.driver.sdk.ui.components.elements.tree
import com.intellij.driver.sdk.ui.xQuery
import org.junit.jupiter.api.Assertions
import kotlin.time.Duration.Companion.minutes

private val expandedTreeForProject = mutableSetOf<String>()

/**
 * @param expectedStatus e.g., `listOf("2 tests passed", "2 tests total")`
 * @param expectedTree list of test tree elements from top to bottom, disregarding the actual tree structure
 */
fun IdeaFrameUI.verifyTestStatus(expectedStatus: List<String>, expectedTree: Collection<String>) {
  step("Verify test status") {
    waitContainsText("Test Results", timeout = 1.minutes)
    val actualStatus = x { byClass("TestStatusLine") }.getAllTexts().filterRelevant()
    Assertions.assertEquals(expectedStatus, actualStatus, "Test status must match")
  }
  step("Verify test results tree") {
    if (expandedTreeForProject.add(project.toString())) {
      x { byAccessibleName("Show Passed") }.click()
    }
    val treeComponent = testTreeView()
    treeComponent.expandAll()
    val actualTree = treeComponent.getAllTexts().filterRelevant()

    when (expectedTree) {
      is Set -> Assertions.assertEquals(expectedTree, actualTree.toSet(), "Test result tree must match")
      is List -> Assertions.assertEquals(expectedTree, actualTree, "Test result tree must match")
      else -> Assertions.fail("expectedTree must be Set or List")
    }
  }
}

fun IdeaFrameUI.testTreeView(): JTreeUiComponent = tree(xQuery { byClass("SMTRunnerTestTreeView") })

private fun List<UiText>.filterRelevant() =
  this
    .map { it.text }
    .filter { " sec" !in it && " ms" !in it && it != "Test Results" }
