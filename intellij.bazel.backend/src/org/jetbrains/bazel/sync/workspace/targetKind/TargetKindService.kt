/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.sync.workspace.targetKind

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind

/**
 * Provides a set of recognized bazel rule names. Individual language-specific sub-plugins can use
 * this EP to register rule types relevant to that language.
 *
 *
 * Each rule name must map to at most one Kind, across all such providers.
 */
@ApiStatus.Internal
interface TargetKindProvider {
  /** A set of rule names known at compile time.  */
  val targetKinds: Set<TargetKind>

  companion object {
    val ep: ExtensionPointName<TargetKindProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.targetKindProvider")
  }
}

/**
 * We cache target kinds provided by the extension points above. This state is associated with an
 * application, so for example needs to be reset between unit test runs with manual extension
 * point setup.
 */
@Service(Service.Level.APP)
@ApiStatus.Internal
class TargetKindService {
  /** An internal map of all known rule types.  */
  private val stringToKind: Map<String, TargetKind> = TargetKindProvider.ep.extensions
    .flatMap { it.targetKinds }
    .associateBy { it.kind }

  fun findPredefinedRule(ruleName: String): TargetKind? {
    var ruleName = ruleName
    if (ruleName.startsWith(RULE_NAME_PREFIX_TRANSITION)) {
      ruleName = ruleName.substring(RULE_NAME_PREFIX_TRANSITION.length)
    }
    return stringToKind[ruleName]
  }

  fun guessFromRuleName(ruleName: String): TargetKind {
    return findPredefinedRule(ruleName)
           ?: TargetKind(kind = ruleName, ruleType = guessRuleType(ruleName))
  }

  /** If rule type isn't recognized, uses a heuristic to guess the rule type. */
  private fun guessRuleType(ruleName: String): RuleType {
    val kind = findPredefinedRule(ruleName)
    return when {
      kind != null -> kind.ruleType
      isTestSuite(ruleName) || ruleName.endsWith("_test") -> RuleType.TEST
      ruleName.endsWith("_binary") -> RuleType.BINARY
      else -> RuleType.LIBRARY
    }
  }

  private fun isTestSuite(ruleName: String): Boolean {
    // handle plain test_suite targets and macros producing a test/test_suite
    return ruleName.endsWith("test_suite") || ruleName.endsWith("test_suites")
  }

  @ApiStatus.Internal
  companion object {
    fun getInstance(): TargetKindService = service()

    /**
     * When a rule has a transition applied to it then it will present with this
     * prefix on it.
     */
    private const val RULE_NAME_PREFIX_TRANSITION = "_transition_"
  }
}
