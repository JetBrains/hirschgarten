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
package org.jetbrains.bazel.ogRun.other

import com.google.common.collect.ImmutableMap

/** The "size" attribute from test rules  */
enum class TestSize(private val sizeName: String) {
  SMALL("small"),
  MEDIUM("medium"),
  LARGE("large"),
  ENORMOUS("enormous"),
  ;

  companion object {
    // Rules are "medium" test size by default
    val DEFAULT_RULE_TEST_SIZE: TestSize = TestSize.MEDIUM

    // Non-annotated methods and classes are "small" by default
    val DEFAULT_NON_ANNOTATED_TEST_SIZE: TestSize = TestSize.SMALL

    private val STRING_TO_SIZE = makeStringToSizeMap()

    fun fromString(string: String): TestSize? = STRING_TO_SIZE[string]

    private fun makeStringToSizeMap(): ImmutableMap<String?, TestSize?> {
      val result = ImmutableMap.builder<String?, TestSize?>()
      for (size in TestSize.entries) {
        result.put(size.sizeName, size)
      }
      return result.build()
    }
  }
}
