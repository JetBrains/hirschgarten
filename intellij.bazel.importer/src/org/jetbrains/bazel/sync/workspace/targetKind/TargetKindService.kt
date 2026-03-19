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
import org.jetbrains.bazel.info.BspTargetInfo
import java.util.Collections

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

  /**
   * A heuristic to identify additional target kinds at runtime which aren't known up-front. For
   * example, any rule name starting with 'kt_jvm_' might be parsed as a kotlin rule of unknown
   * [RuleType].
   */
  fun inferTargetKind(target: BspTargetInfo.TargetInfo): TargetKind? = null

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
  private val stringToKind: MutableMap<String, TargetKind> =
    Collections.synchronizedMap(HashMap())

  init {
    // initialize the global state
    TargetKindProvider.ep.extensions
      .flatMap { it.targetKinds }
      .forEach { this.cacheIfNecessary(it) }
  }

  /** Add the Kind instance to the global map, or returns an existing kind with this rule name.  */
  fun cacheIfNecessary(kind: TargetKind): TargetKind {
    val existing = stringToKind.putIfAbsent(kind.kind, kind)
    if (existing != null) {
      return existing
    }
    return kind
  }

  fun fromRuleName(ruleName: String): TargetKind? {
    var ruleName = ruleName
    if (ruleName.startsWith(RULE_NAME_PREFIX_TRANSITION)) {
      ruleName = ruleName.substring(RULE_NAME_PREFIX_TRANSITION.length)
    }
    return stringToKind[ruleName]
  }

  fun fromTargetInfo(target: BspTargetInfo.TargetInfo): TargetKind {
    fromRuleName(target.kind)?.let { return it }
    TargetKindProvider.ep
      .lazySequence()
      .firstNotNullOfOrNull { it.inferTargetKind(target) }
      ?.let { return it }
    return TargetKind(target.kind, languageClasses = emptySet(), RuleType.LIBRARY)
  }

  fun guessFromRuleName(ruleName: String): TargetKind {
    fromRuleName(ruleName)?.let { return it }
    val ruleType = guessRuleType(ruleName)
    return TargetKind(kind = ruleName, languageClasses = emptySet(), ruleType = ruleType)
  }

  /** If rule type isn't recognized, uses a heuristic to guess the rule type. */
  private fun guessRuleType(ruleName: String): RuleType {
    val kind = fromRuleName(ruleName)
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
