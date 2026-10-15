// Copyright 2000-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
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

package org.jetbrains.bazel.clion.run

import com.intellij.execution.Location
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.bazel.run.test.BazelTestLocatorProvider
import org.jetbrains.bazel.run.test.CompositeModificationTracker

/**
 * Resolves the generic [BAZEL_CC_PROTOCOL] location URLs asking every registered [BazelCcTestFrameworkSupport] in turn.
 */
private object BazelCcTestLocator : SMTestLocator, DumbAware {
  override fun getLocation(
    protocol: String,
    path: String,
    project: Project,
    scope: GlobalSearchScope,
  ): List<Location<*>> {
    if (protocol != BAZEL_CC_PROTOCOL) return emptyList()

    return BazelCcTestFrameworkSupport.EP_NAME.extensionList
             .firstNotNullOfOrNull { navigable(it.locate(path, project, scope)) }
           ?: emptyList()
  }

  /** Discards results which resolved only to a non-navigable re-run placeholder. */
  private fun navigable(locations: List<Location<*>>): List<Location<*>>? {
    return locations.takeIf { list -> list.any { hasFile(it) } }
  }

  private fun hasFile(location: Location<*>): Boolean {
    return location.virtualFile != null
  }

  override fun getLocationCacheModificationTracker(project: Project): ModificationTracker =
    CompositeModificationTracker(BazelCcTestFrameworkSupport.EP_NAME.extensionList.map { it.getLocationCacheModificationTracker(project) })
}

internal class BazelCcTestLocatorProvider : BazelTestLocatorProvider {
  override fun getTestLocator(): SMTestLocator = BazelCcTestLocator
}
