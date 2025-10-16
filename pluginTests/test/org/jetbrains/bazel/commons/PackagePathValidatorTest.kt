
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

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class PackagePathValidatorTest {

  @Test
  fun testPassingValidations() {
    PackagePathValidator.validatePackageName("foo").shouldBeNull()
    PackagePathValidator.validatePackageName("foo_123").shouldBeNull()
    PackagePathValidator.validatePackageName("Foo").shouldBeNull()
    PackagePathValidator.validatePackageName("FOO").shouldBeNull()
    PackagePathValidator.validatePackageName("foO").shouldBeNull()
    PackagePathValidator.validatePackageName("foo-bar").shouldBeNull()
    PackagePathValidator.validatePackageName("Foo-Bar").shouldBeNull()
    PackagePathValidator.validatePackageName("FOO-BAR").shouldBeNull()
    PackagePathValidator.validatePackageName("bar.baz").shouldBeNull()
    PackagePathValidator.validatePackageName("a/..b").shouldBeNull()
    PackagePathValidator.validatePackageName("a/.b").shouldBeNull()
    PackagePathValidator.validatePackageName("a/b.").shouldBeNull()
    PackagePathValidator.validatePackageName("a/b..").shouldBeNull()
    PackagePathValidator.validatePackageName("a$( )/b..").shouldBeNull()
  }

  @Test
  fun testFailingValidations() {
    PackagePathValidator.validatePackageName("/foo") shouldContain "package names may not start with '/'"
    PackagePathValidator.validatePackageName("foo/") shouldContain "package names may not end with '/'"
    PackagePathValidator.validatePackageName("foo:bar") shouldContain PackagePathValidator.PACKAGE_NAME_ERROR
    PackagePathValidator.validatePackageName("baz@12345") shouldContain PackagePathValidator.PACKAGE_NAME_ERROR

    PackagePathValidator.validatePackageName("bar/../baz") shouldContain PackagePathValidator.PACKAGE_NAME_DOT_ERROR
    PackagePathValidator.validatePackageName("bar/..") shouldContain PackagePathValidator.PACKAGE_NAME_DOT_ERROR
    PackagePathValidator.validatePackageName("../bar") shouldContain PackagePathValidator.PACKAGE_NAME_DOT_ERROR
    PackagePathValidator.validatePackageName("bar/...") shouldContain PackagePathValidator.PACKAGE_NAME_DOT_ERROR

    PackagePathValidator.validatePackageName("bar/./baz") shouldContain PackagePathValidator.PACKAGE_NAME_DOT_ERROR
    PackagePathValidator.validatePackageName("bar/.") shouldContain PackagePathValidator.PACKAGE_NAME_DOT_ERROR
    PackagePathValidator.validatePackageName("./bar") shouldContain PackagePathValidator.PACKAGE_NAME_DOT_ERROR
  }
}
