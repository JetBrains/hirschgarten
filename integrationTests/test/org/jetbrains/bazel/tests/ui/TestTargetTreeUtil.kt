package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.elements.tree
import com.intellij.driver.sdk.ui.xQuery
import org.junit.jupiter.api.Assertions
import kotlin.time.Duration.Companion.minutes

/**
 * @param expectedStatus e.g., `listOf("2 tests passed", "2 tests total")`
 * @param expectedTree list of test tree elements from top to bottom, disregarding the actual tree structure
 */
fun IdeaFrameUI.verifyTestStatus(expectedStatus: List<String>, expectedTree: List<String>) {
  var result = true
  step("Verify test status") {
    waitContainsText("Test Results", timeout = 1.minutes)
    val actualResults = x("//div[@class='TestStatusLine']").getAllTexts()
    for (expectedItem in expectedStatus) {
      result = result && actualResults.any { it.text.contains(expectedItem) }
    }
    Assertions.assertTrue(
      result,
      "Actual status doesn't contain expected: $expectedStatus",
    )
  }
  step("Verify test results tree") {
    x("//div[@accessiblename='Show Passed']").click()
    val treeResults = tree(xQuery { byClass("SMTRunnerTestTreeView") }).getAllTexts()
    for (expectedItem in expectedTree) {
      result = result && treeResults.any { it.text.contains(expectedItem) }
    }
    Assertions.assertTrue(
      result,
      "Actual tree doesn't contain expected: $expectedTree",
    )
  }
}
