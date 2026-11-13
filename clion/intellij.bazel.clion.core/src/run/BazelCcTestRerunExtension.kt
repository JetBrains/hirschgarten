// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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

import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.cidr.execution.testing.CidrTestLocation
import org.jetbrains.bazel.run.BazelTestFilterRerunExtension
import org.jetbrains.bazel.run.config.BazelRunConfiguration

internal class BazelCcTestRerunExtension : BazelTestFilterRerunExtension() {
  override fun isApplicable(configuration: BazelRunConfiguration): Boolean =
    configuration.handler is CcTestHandler

  override fun getTestFilter(configuration: BazelRunConfiguration, tests: List<AbstractTestProxy>): String? {
    val project = configuration.project
    val testLocations = tests.mapNotNull { it.getLocation(project, GlobalSearchScope.allScope(project)) }
    // The location knows the instantiation and the parameter of a test, but its PSI element does not
    val testsAndFrameworks = testLocations.filterIsInstance<CidrTestLocation<*>>().mapNotNull { location ->
      val frameworkSupport = BazelCcTestFrameworkSupport.findTestAndFramework(location.psiElement)?.second ?: return@mapNotNull null
      location.testScopeElement to frameworkSupport
    }
    // a single Bazel invocation carries one --test_filter, which is framework-specific
    val frameworkSupport = testsAndFrameworks.map { it.second }.distinct().singleOrNull() ?: return null
    return frameworkSupport.createTestFilter(testsAndFrameworks.map { it.first })
  }
}
