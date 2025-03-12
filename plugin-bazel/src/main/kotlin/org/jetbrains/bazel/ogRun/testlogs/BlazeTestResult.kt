/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.testlogs

import com.google.auto.value.AutoValue
import com.google.common.collect.ImmutableSet

/** The result of a single blaze test action.  */
@AutoValue
abstract class BlazeTestResult {
  /** Status for a single blaze test action.  */
  enum class TestStatus {
    NO_STATUS,
    PASSED,
    FLAKY,
    TIMEOUT,
    FAILED,
    INCOMPLETE,
    REMOTE_FAILURE,
    FAILED_TO_BUILD,
    TOOL_HALTED_BEFORE_TESTING,
  }

  @JvmField
  abstract val label: Label?

  @JvmField
  abstract val targetKind: Kind?

  @JvmField
  abstract val testStatus: TestStatus?

  @JvmField
  abstract val outputXmlFiles: ImmutableSet<out BlazeArtifact>?

  companion object {
    /** The set of statuses for which no useful output XML is written.  */
    @JvmField
    val NO_USEFUL_OUTPUT: ImmutableSet<TestStatus?> =
      ImmutableSet.of<TestStatus?>(
        TestStatus.TIMEOUT,
        TestStatus.REMOTE_FAILURE,
        TestStatus.FAILED_TO_BUILD,
        TestStatus.TOOL_HALTED_BEFORE_TESTING,
      )

    fun create(
      label: Label?,
      targetKind: Kind?,
      testStatus: TestStatus?,
      outputXmlFiles: ImmutableSet<out BlazeArtifact?>?,
    ): BlazeTestResult = AutoValue_BlazeTestResult(label, targetKind, testStatus, outputXmlFiles)
  }
}
