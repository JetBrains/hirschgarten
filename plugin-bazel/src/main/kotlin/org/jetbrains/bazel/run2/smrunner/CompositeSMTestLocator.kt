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
package org.jetbrains.bazel.run2.smrunner

import com.google.common.collect.ImmutableList
import com.intellij.execution.Location
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

/** Combines multiple language-specific [SMTestLocator]s.  */
class CompositeSMTestLocator(private val locators: List<SMTestLocator>) : SMTestLocator {
  // Super method uses raw Location. Check super method again after #api212.
  override fun getLocation(
    protocol: String, path: String, project: Project, scope: GlobalSearchScope
  ): List<Location<*>> {
    for (locator in locators) {
      val result = locator.getLocation(protocol, path, project, scope)
      if (!result.isEmpty()) {
        return result
      }
    }
    return emptyList()
  }
}
