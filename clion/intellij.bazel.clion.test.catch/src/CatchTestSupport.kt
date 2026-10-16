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
package org.jetbrains.bazel.clion.test.catch

import com.intellij.execution.Location
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.cidr.execution.testing.CidrTestLanguageSupport
import com.jetbrains.cidr.execution.testing.CidrTestScopeElement
import com.jetbrains.cidr.execution.testing.tcatch.CatchTestLanguageSupport
import com.jetbrains.cidr.execution.testing.tcatch.CidrCatchTestLinker
import com.jetbrains.cidr.execution.testing.tcatch.CidrCatchTestLocationProvider
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.run.BazelCcTestFrameworkSupport
import org.jetbrains.bazel.clion.run.TEST_NAME_PARTS_SPLITTER

@ApiStatus.Internal
class CatchTestSupport : BazelCcTestFrameworkSupport {
  override val framework: CidrTestLanguageSupport
    get() = CatchTestLanguageSupport.getInstance()

  override fun locate(path: String, project: Project, scope: GlobalSearchScope): List<Location<*>> {

    val test = path.substringAfter(TEST_NAME_PARTS_SPLITTER)

    val linker = CidrCatchTestLinker.create(test)

    return CidrCatchTestLocationProvider.INSTANCE.getLocation(
      CidrCatchTestLocationProvider.PROTOCOL, linker.findPath, project, scope,
    )
  }

  /** Catch2 accepts a comma-separated list of test case names. */
  override fun createTestFilter(tests: List<CidrTestScopeElement>): String? {
    return tests.map { it.pattern }.ifEmpty { null }?.joinToString(",")
  }

  override fun getLocationCacheModificationTracker(project: Project): ModificationTracker =
    CidrCatchTestLocationProvider.INSTANCE.getLocationCacheModificationTracker(project)
}
