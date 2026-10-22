package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.UiText
import com.intellij.driver.sdk.ui.components.UiComponent
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.GutterUiComponent
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.toolwindows.debugToolWindow
import com.intellij.driver.sdk.ui.components.elements.ActionButtonUi
import com.intellij.driver.sdk.ui.components.elements.JTreeUiComponent
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.components.elements.tree
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.waitFor
import com.intellij.driver.sdk.withRetries
import com.intellij.ide.starter.ide.IDETestContext
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.junit.jupiter.api.Assertions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * If we have too many test results, then part of the tree may be cut off because it doesn't fit into the screen
 */
private const val TREE_SIZE_THAT_FITS_INTO_SCREEN = 6

/**
 * @param expectedStatus e.g., `listOf("2 tests passed", "2 tests total")`
 * @param expectedTree list of test tree elements from top to bottom, disregarding the actual tree structure
 */
fun IdeaFrameUI.verifyTestStatus(expectedStatus: List<String>, expectedTree: Collection<String>) {
  waitForIndicators(5.minutes)  // Wait for Bazel build to finish
  waitNoTexts("Instantiating tests…", timeout = 5.minutes)
  step("Verify test status") {
    waitContainsText("Test Results", timeout = 1.minutes)
    // The status line fills in after the "Test Results" tab opens, so one read can see an empty or partial line.
    waitFor(
      message = "The test status is $expectedStatus",
      timeout = 1.minutes,
      interval = 1.seconds,
      errorMessage = { actualStatus -> "Test status must match. Expected: $expectedStatus. Actual: $actualStatus" },
      getter = { x { byClass("TestStatusLine") }.getAllTexts().filterRelevant() },
      checker = { actualStatus -> actualStatus == expectedStatus },
    )
  }
  step("Verify test results tree") {
    val showPassed = x(ActionButtonUi::class.java) { byAccessibleName("Show Passed") }
    if (!showPassed.isSelected) {
      showPassed.click()
    }
    val treeComponent = testTreeView()
    treeComponent.expandAll()
    val actualTree = treeComponent.getAllTexts().filterRelevant()
    val expectedTree = if (expectedTree.size > TREE_SIZE_THAT_FITS_INTO_SCREEN && actualTree.size >= TREE_SIZE_THAT_FITS_INTO_SCREEN) {
      expectedTree.take(actualTree.size)
    } else {
      expectedTree
    }

    when (expectedTree) {
      is Set -> Assertions.assertEquals(expectedTree, actualTree.toSet(), "Test result tree must match")
      is List -> Assertions.assertEquals(expectedTree, actualTree, "Test result tree must match")
      else -> Assertions.fail("expectedTree must be Set or List")
    }
  }
}

/**
 * Right-clicks the one tree row that matches [matches], and returns the context menu that opens.
 *
 * `JTreeUiComponent.rightClickRow` looks the row up once. It fails when the tree is still rebuilding after a sync,
 * and when more than one row matches. A right click can also open no menu. So this helper waits for exactly one
 * matching row, clicks it, and clicks again when [menu] does not appear.
 *
 * Pass `{ popupMenu() }` from the scope of the IDE frame as [menu].
 */
fun <T : UiComponent> JTreeUiComponent.rightClickRowAndWaitForMenu(rowName: String, matches: (String) -> Boolean, menu: () -> T): T =
  withRetries(message = "Open the context menu of the row '$rowName'", times = CONTEXT_MENU_ATTEMPTS) {
    waitFor(
      message = "Exactly one tree row matches '$rowName'",
      timeout = 1.minutes,
      interval = 1.seconds,
      errorMessage = { rows -> "Expected one row for '$rowName'. The tree has these rows: $rows" },
      getter = { collectExpandedPaths().map { it.path.last() } },
      checker = { rows -> rows.count(matches) == 1 },
    )
    rightClickRow(matches)
    menu().waitFound(10.seconds)
  }

private const val CONTEXT_MENU_ATTEMPTS = 3

fun IdeaFrameUI.testTreeView(): JTreeUiComponent = tree(xQuery { byClass("SMTRunnerTestTreeView") })

private fun List<UiText>.filterRelevant() =
  this
    .map { it.text }
    .filter { " sec" !in it && " ms" !in it && it != "Test Results" }

fun IDETestContext.setRunConfigRunWithBazel(runTestWithBazel: Boolean): IDETestContext = applyVMOptionsPatch {
  addSystemProperty(BazelFeatureFlags.RUN_CONFIG_RUN_WITH_BAZEL, runTestWithBazel.toString())
}

fun IDETestContext.setAutoOpenProjectIfPresent(autoOpenProjectIfPresent: Boolean): IDETestContext = applyVMOptionsPatch {
  addSystemProperty(BazelFeatureFlags.AUTO_OPEN_PROJECT_IF_PRESENT, autoOpenProjectIfPresent.toString())
}


/**
 * [line] can be different depending on e.g. imports folding
 */
fun IdeaFrameUI.clickTestGutterOnLine(line: Int) {
  clickRunGutterOnLine(line)
  val allRunTexts = popup().getAllTexts { it.text.startsWith("Run ") }.sortedBy { it.point.y }
  check(allRunTexts.isNotEmpty()) {
    "Couldn't find a text in the popup starting with 'Run ', got: ${
      popup().getAllTexts().map { it.text }
    }"
  }
  allRunTexts.first().click()
}

/**
 * [line] can be different depending on e.g. imports folding
 */
fun IdeaFrameUI.clickRunGutterOnLine(line: Int) {
  keyboard { escape() }  // close a previous run gutter if one exists
  val runGutter = getRunGutterOnLine(line)
  runGutter.click()
}

private val GUTTER_ICONS_TIMEOUT = 30.seconds

/** Returns the gutter icons of the selected editor after at least one icon appears. */
fun IdeaFrameUI.waitForGutterIcons(): List<GutterUiComponent.GutterIcon> {
  val gutter = editorTabs().gutter()
  return waitFor(
    message = "Gutter icons in the selected editor",
    timeout = GUTTER_ICONS_TIMEOUT,
    getter = { gutter.icons },
    checker = { it.isNotEmpty() },
  )
}

fun IdeaFrameUI.getRunGutterOnLine(line: Int): GutterUiComponent.GutterIcon {
  val gutter = editorTabs().gutter()
  val icons = waitFor(
    message = "Run gutter on line $line",
    timeout = GUTTER_ICONS_TIMEOUT,
    errorMessage = { icons -> "No run gutter on line $line, gutter icons are on lines ${icons.map { it.line }.sorted()}" },
    getter = { gutter.icons },
    checker = { icons -> icons.any { it.line == line } },
  )
  // Run gutter icons can interfere with annotations, but they are displayed first
  return icons.filter { it.line == line }.minBy { it.location.x }
}

fun IdeaFrameUI.verifyAvailableRunGutterActions(texts: List<String>) {
  texts.forEach { text ->
    popup().waitAnyTextsContains(text)
  }
  val allTexts = popup().getAllTexts().sortedBy { it.point.y }.map { it.text }.filter {
    it != "Modify Run Configuration…" &&
    "Ctrl" !in it && "⌃" !in it  // filter out key combinations
  }
  check(allTexts.size == texts.size) {
    "Too many texts! Expected: $texts, actual: $allTexts"
  }
  check(allTexts.zip(texts).all { (expected, actual) -> expected in actual }) {
    "Texts are correct, but the order is wrong! Expected $texts, actual: $allTexts"
  }
}

val IdeaFrameUI.debuggerFramesUi
  get() = x("//div[@class='XDebuggerFramesList']")

val IdeaFrameUI.consoleView
  get() = x("//div[@class='ConsoleViewImpl']")

private val RUN_CONSOLE_TIMEOUT = 3.minutes

/**
 * Waits until a console in the IDE frame shows a line that contains [text].
 *
 * A Bazel run first builds its target in the Build tool window, and its console appears only when the run starts.
 * So the wait covers the build, and it looks up the consoles again on each poll.
 */
fun IdeaFrameUI.waitForConsoleText(text: String, timeout: Duration = RUN_CONSOLE_TIMEOUT) {
  waitFor(
    message = "A console shows '$text'",
    timeout = timeout,
    interval = 1.seconds,
    errorMessage = { lines -> "No console shows '$text'. The last console lines: ${lines.takeLast(10)}" },
    getter = {
      xx("//div[@class='ConsoleViewImpl']").list().flatMap { console ->
        runCatching { console.getAllTexts().map { it.text } }.getOrDefault(emptyList())
      }
    },
    checker = { lines -> lines.any { text in it } },
  )
}

private data class DebuggerPauseState(
  val toolWindowPresent: Boolean = false,
  val resumeEnabled: Boolean = false,
  val frames: List<String> = emptyList(),
)

fun IdeaFrameUI.waitForDebuggerPausedAt(vararg expectedFrameTexts: String, timeout: Duration = 3.minutes) {
  require(expectedFrameTexts.isNotEmpty())
  step("Wait for debugger to pause at ${expectedFrameTexts.toList()}") {
    waitFor(
      message = "Debugger should pause at ${expectedFrameTexts.toList()}",
      timeout = timeout,
      interval = 1.seconds,
      errorMessage = { state ->
        "Debugger did not pause at ${expectedFrameTexts.toList()}. " +
          "Tool window present: ${state.toolWindowPresent}, resume enabled: ${state.resumeEnabled}, frames: ${state.frames}"
      },
      getter = {
        val toolWindow = debugToolWindow()
        if (!toolWindow.present()) {
          DebuggerPauseState()
        }
        else {
          val threadsAndVariablesTab = toolWindow.threadsAndVariablesTab
          if (!threadsAndVariablesTab.present()) {
            DebuggerPauseState(toolWindowPresent = true)
          }
          else {
            threadsAndVariablesTab.click()
            val resumeButton = toolWindow.resumeButton
            val framesUi = debuggerFramesUi
            DebuggerPauseState(
              toolWindowPresent = true,
              resumeEnabled = resumeButton.present() && resumeButton.isEnabled(),
              frames = if (framesUi.present()) framesUi.getAllTexts().map { it.text } else emptyList(),
            )
          }
        }
      },
      checker = { state ->
        state.resumeEnabled && expectedFrameTexts.any { expected -> state.frames.any { expected in it } }
      },
    )
  }
}
