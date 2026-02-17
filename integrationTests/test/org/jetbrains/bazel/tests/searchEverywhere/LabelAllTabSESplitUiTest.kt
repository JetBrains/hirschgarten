package org.jetbrains.bazel.tests.searchEverywhere

import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.mainToolbar
import com.intellij.driver.sdk.ui.components.common.popups.searchEverywherePopup
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.actionSystem.IdeActions
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LabelAllTabSESplitUiTest : IdeStarterBaseProjectTest() {
  private val searchText = "//python:binary"
  private val elements = listOf("//python:binary")

  @Test
  fun `search everywhere should show Bazel labels when contributor enabled`() {
    val context = createContext("labelAllTabSESplit", IdeaBazelCases.LabelAllTabSESplit).enableSplitSearchEverywhere()
    context
      .runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          performSearchEverywhereActions(
            searchText,
            "labels",
            elements,
            contributors = listOf("Bazel"),
            enableContributors = true,
            expectResults = true,
          )
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `search everywhere should hide Bazel labels when contributor disabled`() {
    val context = createContext("labelAllTabSESplit", IdeaBazelCases.LabelAllTabSESplit).enableSplitSearchEverywhere()
    context
      .runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          performSearchEverywhereActions(
            searchText,
            "labels",
            elements,
            contributors = listOf("Bazel"),
            enableContributors = false,
            expectResults = false,
          )
        }
      }
    checkIdeaLogForExceptions(context)
  }

  fun IdeaFrameUI.performSearchEverywhereActions(
    searchText: String,
    elementsDisplayType: String,
    elements: List<String>,
    elementsShortNames: List<String> = elements,
    contributors: List<String>,
    enableContributors: Boolean,
    expectResults: Boolean,
  ) {
    mainToolbar.click()
    driver.invokeAction(IdeActions.ACTION_SEARCH_EVERYWHERE)
    searchEverywherePopup(isSplit = true) {
      step(
        if (enableContributors)
          "Disable all contributors except ${contributors.joinToString(", ")} in the type filter"
        else
          "Disable ${contributors.joinToString(", ")} in the type filter",
      ) {
        if (enableContributors) {
          clickNoneButtonInTypeFilters()
        }
        switchTypeFilters(contributors)
      }

      step("Type \"$searchText\"") {
        searchField.text = searchText

        val joinedElements = elementsShortNames.joinedElementsForMessage()
        val fullMessage = "$joinedElements $elementsDisplayType are not in the results list"

        if (expectResults) {
          waitFor(fullMessage, 15.seconds) {
            val items = resultsList.items.map { it.trim() }
            items.containsAll(elements)
          }
        } else {
          waitFor(fullMessage, 15.seconds) {
            resultsList.items.isNotEmpty() || hasText("Nothing found.")
          }
          Assertions.assertTrue(elements.all { !resultsList.items.contains(it) })
        }
      }
    }
  }

  private fun List<String>.joinedElementsForMessage(): String =
    map { "\"$it\"" }.let { names ->
      if (names.size == 1) {
        names[0]
      } else {
        names.dropLast(1).joinToString(", ") + " and " + names.last()
      }
    }
}
