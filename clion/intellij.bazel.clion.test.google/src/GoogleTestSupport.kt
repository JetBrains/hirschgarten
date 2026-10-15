// Copyright 2000-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.clion.test.google

import com.intellij.execution.Location
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.cidr.execution.testing.CidrTestLanguageSupport
import com.jetbrains.cidr.execution.testing.CidrTestScopeElement
import com.jetbrains.cidr.execution.testing.google.CidrGoogleTestLinker
import com.jetbrains.cidr.execution.testing.google.CidrGoogleTestLocationProvider
import com.jetbrains.cidr.execution.testing.google.GoogleTestLanguageSupport
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.run.BazelCcTestFrameworkSupport
import org.jetbrains.bazel.clion.run.TEST_NAME_PARTS_SPLITTER

@ApiStatus.Internal
class GoogleTestSupport : BazelCcTestFrameworkSupport {
  override val framework: CidrTestLanguageSupport
    get() = GoogleTestLanguageSupport.getInstance()

  override fun locate(path: String, project: Project, scope: GlobalSearchScope): List<Location<*>> {
    val suite = path.substringBefore(TEST_NAME_PARTS_SPLITTER).ifEmpty { null }
    val test = path.substringAfter(TEST_NAME_PARTS_SPLITTER)
    val linker = CidrGoogleTestLinker.parseTestOut("$suite.$test") ?: return emptyList()
    return CidrGoogleTestLocationProvider.INSTANCE.getLocation(
      CidrGoogleTestLocationProvider.PROTOCOL, linker.path, project, scope,
    )
  }

  /** Google Test accepts a colon-separated list of patterns. */
  override fun createTestFilter(tests: List<CidrTestScopeElement>): String? {
    if (tests.isEmpty()) {
      return null
    }

    return tests.joinToString(":") { it.pattern }
  }

  override fun getLocationCacheModificationTracker(project: Project): ModificationTracker =
    CidrGoogleTestLocationProvider.INSTANCE.getLocationCacheModificationTracker(project)
}
