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

import com.intellij.openapi.extensions.ExtensionPointName

/** Parses test case failure messages to give actual/expected text comparisons.  */
interface TestComparisonFailureParser {
  fun tryParse(message: String): BlazeComparisonFailureData?

  /**
   * Data class for actual/expected text.
   */
  data class BlazeComparisonFailureData(
    @JvmField val actual: String?,
    @JvmField val expected: String?,
  ) {
    companion object {
      @JvmField
      val NONE: BlazeComparisonFailureData = BlazeComparisonFailureData(null, null)
    }
  }

  companion object {
    @JvmStatic
    fun parse(message: String): BlazeComparisonFailureData {
      for (parser in EP_NAME.extensions) {
        val data = parser.tryParse(message)
        if (data != null) {
          return data
        }
      }
      return BlazeComparisonFailureData.NONE
    }

    val EP_NAME: ExtensionPointName<TestComparisonFailureParser> =
      ExtensionPointName.create("com.google.idea.blaze.TestComparisonFailureParser")
  }
}
