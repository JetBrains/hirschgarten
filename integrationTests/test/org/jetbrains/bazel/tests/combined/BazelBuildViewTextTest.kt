package org.jetbrains.bazel.tests.combined

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BazelBuildViewTextTest {
  @Test
  fun `build with one action completes`() {
    assertTrue(BazelBuildViewText.hasInterpretedBuildEnd(ONE_ACTION_BUILD))
    assertFalse(BazelBuildViewText.hasEmptyInfoLine(ONE_ACTION_BUILD))
  }

  @Test
  fun `build with two actions completes`() {
    assertTrue(BazelBuildViewText.hasInterpretedBuildEnd(TWO_ACTIONS_BUILD))
    assertFalse(BazelBuildViewText.hasEmptyInfoLine(TWO_ACTIONS_BUILD))
  }

  @Test
  fun `padding spaces between console lines do not hide the build end`() {
    val text = "INFO: Build completed successfully, 2 total actions          INFO: Build Event Protocol files produced successfully."
    assertTrue(BazelBuildViewText.hasInterpretedBuildEnd(text))
  }

  @Test
  fun `build view of a disabled PTY keeps an empty INFO line`() {
    assertTrue(BazelBuildViewText.hasInterpretedBuildEnd(DISABLED_PTY_BUILD))
    assertTrue(BazelBuildViewText.hasEmptyInfoLine(DISABLED_PTY_BUILD))
  }

  @Test
  fun `unfinished build does not complete`() {
    assertFalse(BazelBuildViewText.hasInterpretedBuildEnd("INFO: Found 1 target...[1 / 2] Compiling Kotlin //:associates_example"))
  }
}

private const val ONE_ACTION_BUILD =
  "INFO: Found 1 target...Target //:associates_example up-to-date:  bazel-bin/associates_example.jar  " +
  "bazel-bin/associates_example.jdeps               INFO: Elapsed time: 17.831s, Critical Path: 0.67s" +
  "INFO: 1 process: 1 internal.INFO: Build completed successfully, 1 total action               " +
  "INFO: Build Event Protocol files produced successfully.                  Command completed in 18.4s (exit code 0)"

private const val TWO_ACTIONS_BUILD =
  "INFO: Found 1 target...                 Target //:associates_example up-to-date:                        " +
  "bazel-bin/associates_example.jar                                                                       " +
  "bazel-bin/associates_example.jdepsINFO: Elapsed time: 0.126s, Critical Path: 0.01s" +
  "INFO: 2 processes: 1 disk cache hit, 1 internal.INFO: Build completed successfully, 2 total actions" +
  "INFO: Build Event Protocol files produced successfully."

// Constructed text. No recorded build has a disabled PTY.
private const val DISABLED_PTY_BUILD =
  "INFO: INFO: Found 1 target...Target //:associates_example up-to-date:  bazel-bin/associates_example.jar" +
  "INFO: INFO: Build completed successfully, 2 total actionsINFO: Build Event Protocol files produced successfully."
