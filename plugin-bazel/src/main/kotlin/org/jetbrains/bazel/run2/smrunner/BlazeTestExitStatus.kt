/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.run2.smrunner

/** Utilities for interpreting "blaze test" exit codes and associated errors.  */
enum class BlazeTestExitStatus(
  @JvmField val title: String,
  @JvmField val message: String,
  val exitCode: Int,
) {
  // Exit codes taken from
  // https://docs.bazel.build/versions/master/guide.html#what-exit-code-will-i-get
  ALL_TESTS_PASSED("All Tests Passed", "All tests passed.", 0),
  BUILD_FAILED("Build Failed", "Build failed. No tests were run.", 1),
  SOME_TESTS_FAILED("Some Tests Failed", "Build succeeded but some tests failed.", 3),
  NO_TESTS_FOUND("No Test Found", "Build succeeded but no tests were found.", 4),
  TEST_INTERRUPTED("Test Interrupted", "Test interrupted.", 8),
  KNOWN_ERROR_TEST_PROCESS_RECEIVED_SIGQUIT(
    "Runtime Error",
    "Test runtime terminated unexpectedly. Please try re-running the test",
    131,
  ),
  INTERNAL_BLAZE_ERROR(
    "Unhandled Exception / Internal Bazel Error",
    "Internal Bazel error. No tests were run.",
    37,
  ),
  ;

  companion object {
    @JvmStatic
    fun forExitCode(exitCode: Int): BlazeTestExitStatus? {
      for (exitStatus in BlazeTestExitStatus.entries) {
        if (exitCode == exitStatus.exitCode) {
          return exitStatus
        }
      }
      return null
    }
  }
}
