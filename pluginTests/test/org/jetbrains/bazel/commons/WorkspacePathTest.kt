
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
package org.jetbrains.bazel.commons

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider
import org.jetbrains.bazel.test.framework.annotation.BazelTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@BazelTest
class WorkspacePathTest {

  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
    }
  }

  @Test
  fun testValidation() {
    // Valid workspace paths
    WorkspacePath.isValid("").shouldBeTrue()
    WorkspacePath.isValid("foo").shouldBeTrue()
    WorkspacePath.isValid("foo").shouldBeTrue()
    WorkspacePath.isValid("foo/bar").shouldBeTrue()
    WorkspacePath.isValid("foo/bar/baz").shouldBeTrue()

    // Invalid workspace paths
    WorkspacePath.isValid("/foo").shouldBeFalse()
    WorkspacePath.isValid("//foo").shouldBeFalse()
    WorkspacePath.isValid("/").shouldBeFalse()
    WorkspacePath.isValid("foo/").shouldBeFalse()
    WorkspacePath.isValid("foo:").shouldBeFalse()
    WorkspacePath.isValid(":").shouldBeFalse()
    WorkspacePath.isValid("foo:bar").shouldBeFalse()

    WorkspacePath.validate("/foo") shouldBe "Workspace path must be relative; cannot start with '/': /foo"

    WorkspacePath.validate("/") shouldBe "Workspace path must be relative; cannot start with '/': /"

    WorkspacePath.validate("foo/") shouldBe "Workspace path may not end with '/': foo/"

    WorkspacePath.validate("foo:bar") shouldBe "Workspace path may not contain ':': foo:bar"
  }

  @Test
  fun testStringConcatenationConstructor() {
    val empty = WorkspacePath("")
    val dot = WorkspacePath(".")
    val foo = WorkspacePath("foo")
    val dotBar = WorkspacePath("./bar")

    WorkspacePath(empty, "baz").relativePath() shouldBe "baz"
    WorkspacePath(dot, "baz").relativePath() shouldBe "baz"
    WorkspacePath(foo, "baz").relativePath() shouldBe "foo/baz"
    WorkspacePath(dotBar, "baz").relativePath() shouldBe "./bar/baz"
  }
}
