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
package org.jetbrains.bazel.sync

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bazel.commons.LanguageClass
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
interface TargetKindProvider {
  /** A set of rule names known at compile time.  */
  val targetKinds: Set<TargetKind>

  val targetKindHeuristics: (BspTargetInfo.TargetInfo) -> TargetKind?
    /**
     * A heuristic to identify additional target kinds at runtime which aren't known up-front. For
     * example, any rule name starting with 'kt_jvm_' might be parsed as a kotlin rule of unknown
     * [RuleType].
     */
    get() = { t: BspTargetInfo.TargetInfo -> null }

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
@Service
class TargetKindService {
  /** An internal map of all known rule types.  */
  val stringToKind: MutableMap<String, TargetKind> =
    Collections.synchronizedMap(HashMap())

  /** An internal map of all known rule types.  */
  val perLanguageKinds: Multimap<LanguageClass, TargetKind> =
    Multimaps.synchronizedSetMultimap<LanguageClass, TargetKind>(HashMultimap.create())

  init {
    // initialize the global state
    TargetKindProvider.ep.extensions
      .flatMap { it.targetKinds }
      .forEach { this.cacheIfNecessary(it) }
  }

  /** Add the Kind instance to the global map, or returns an existing kind with this rule name.  */
  fun cacheIfNecessary(kind: TargetKind): TargetKind {
    val existing = stringToKind.putIfAbsent(kind.kindString, kind)
    if (existing != null) {
      return existing
    }
    kind.languageClasses.forEach {
      perLanguageKinds.put(
        it,
        kind,
      )
    }
    return kind
  }

  companion object {
    val service: TargetKindService
      get() =
        ApplicationManager
          .getApplication()
          .getService<TargetKindService>(TargetKindService::class.java)
  }
}

fun TargetKind.isOneOf(vararg kinds: TargetKind): Boolean = kinds.contains(this)

fun TargetKind.hasLanguage(languageClass: LanguageClass): Boolean = this.languageClasses.contains(languageClass)

fun TargetKind.hasAnyLanguageIn(vararg languageClass: LanguageClass): Boolean = languageClass.any { this.languageClasses.contains(it) }

/**
 * When a rule has a transition applied to it then it will present with this
 * prefix on it.
 */
private const val RULE_NAME_PREFIX_TRANSITION = "_transition_"

fun TargetKind.Companion.fromRuleName(ruleName: String): TargetKind? {
  var ruleName = ruleName
  if (ruleName.startsWith(RULE_NAME_PREFIX_TRANSITION)) {
    ruleName = ruleName.substring(RULE_NAME_PREFIX_TRANSITION.length)
  }
  return TargetKindService.service.stringToKind[ruleName]
}

val TargetKind.Companion.perLanguageKinds: Multimap<LanguageClass, TargetKind>
  /**
   * Returns a per-language map of rule kinds handled by an available [TargetKindProvider].
   *
   *
   * Don't rely on this map being complete -- some rule names are recognized at runtime using
   * heuristics.
   */
  get() {
    val state =
      TargetKindService.service
    synchronized(state.perLanguageKinds) {
      return state.perLanguageKinds
    }
  }

fun TargetKind.Companion.getKindsForLanguage(language: LanguageClass): Set<TargetKind> = perLanguageKinds.get(language).toSet()

/** If rule type isn't recognized, uses a heuristic to guess the rule type.  */
fun TargetKind.Companion.guessRuleType(ruleName: String): RuleType? {
  val kind = fromRuleName(ruleName)
  if (kind != null) {
    return kind.ruleType
  }
  if (isTestSuite(ruleName) || ruleName.endsWith("_test")) {
    return RuleType.TEST
  }
  if (ruleName.endsWith("_binary")) {
    return RuleType.BINARY
  }
  if (ruleName.endsWith("_library")) {
    return RuleType.LIBRARY
  }
  return RuleType.UNKNOWN
}

private fun isTestSuite(ruleName: String): Boolean {
  // handle plain test_suite targets and macros producing a test/test_suite
  return ruleName.endsWith("test_suite") || ruleName.endsWith("test_suites")
}
