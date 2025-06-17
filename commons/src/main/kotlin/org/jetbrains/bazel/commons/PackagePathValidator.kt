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

import org.jetbrains.annotations.VisibleForTesting

/** Validates package paths in blaze labels / target expressions.  */
object PackagePathValidator {
  /** Matches characters allowed in package name.  */
  private val ALLOWED_CHARACTERS_IN_PACKAGE_NAME: com.google.common.base.CharMatcher =
    com.google.common.base.CharMatcher
      .inRange('0', '9')
      .or(
        com.google.common.base.CharMatcher
          .inRange('a', 'z'),
      ).or(
        com.google.common.base.CharMatcher
          .inRange('A', 'Z'),
      ).or(
        com.google.common.base.CharMatcher
          .anyOf("/-._ $()"),
      ).precomputed()

  @VisibleForTesting
  const val PACKAGE_NAME_ERROR: String =
    "package names may contain only A-Z, a-z, 0-9, '/', '-', '.', ' ', '$', '(', ')' and '_'"

  @VisibleForTesting
  const val PACKAGE_NAME_DOT_ERROR: String = "package name component contains only '.' characters"

  /**
   * Performs validity checking of the specified package name. Returns null on success or an error
   * message otherwise.
   */
  @JvmStatic
  fun validatePackageName(packageName: String): String? {
    val len = packageName.length
    if (len == 0) {
      // Empty package name (//:foo).
      return null
    }

    if (packageName.get(0) == '/') {
      return wrapError(packageName, "package names may not start with '/'")
    }

    if (!ALLOWED_CHARACTERS_IN_PACKAGE_NAME.matchesAllOf(packageName)) {
      return wrapError(packageName, PACKAGE_NAME_ERROR)
    }

    if (packageName.get(len - 1) == '/') {
      return wrapError(packageName, "package names may not end with '/'")
    }
    // Check for empty or dot-only package segment
    var nonDot = false
    var lastSlash = true
    // Going backward and marking the last character as being a / so we detect
    // '.' only package segment.
    for (i in len - 1 downTo -1) {
      val c = if (i >= 0) packageName.get(i) else '/'
      if (c == '/') {
        if (lastSlash) {
          return wrapError(packageName, "package names may not contain '//' path separators")
        }
        if (!nonDot) {
          return wrapError(packageName, PACKAGE_NAME_DOT_ERROR)
        }
        nonDot = false
        lastSlash = true
      } else {
        if (c != '.') {
          nonDot = true
        }
        lastSlash = false
      }
    }
    return null
  }

  private fun wrapError(packageName: String, error: String): String = String.format("Invalid package name '%s': %s", packageName, error)
}
