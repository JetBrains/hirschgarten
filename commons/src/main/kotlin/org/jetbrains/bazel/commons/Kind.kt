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
package org.jetbrains.bazel.commons

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bazel.info.BspTargetInfo
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

/**
 * A set of recognized blaze rule names, together with [LanguageClass] and [RuleType].
 * Language-specific extensions can provide their own set of rule names, as well as heuristics to
 * recognize rule names at runtime.
 *
 *
 * Each rule name maps to at most one Kind.
 */
data class Kind(
  val kindString: String,
  val languageClasses: Set<LanguageClass>,
  val ruleType: RuleType,
) {
  /**
   * Provides a set of recognized blaze rule names. Individual language-specific sub-plugins can use
   * this EP to register rule types relevant to that language.
   *
   *
   * Each rule name must map to at most one Kind, across all such providers.
   */
  interface Provider {
    /** A set of rule names known at compile time.  */
    val targetKinds: Set<Kind>

    val targetKindHeuristics: Function<BspTargetInfo.TargetInfo, Kind?>
      /**
       * A heuristic to identify additional target kinds at runtime which aren't known up-front. For
       * example, any rule name starting with 'kt_jvm_' might be parsed as a kotlin rule of unknown
       * [RuleType].
       */
      get() = Function { t: BspTargetInfo.TargetInfo -> null }

    companion object {
      val EP_NAME: ExtensionPointName<Provider> =
        ExtensionPointName.create<Provider>("com.google.idea.blaze.TargetKindProvider")
    }
  }

  /**
   * We cache target kinds provided by the extension points above. This state is associated with an
   * application, so for example needs to be reset between unit test runs with manual extension
   * point setup.
   */
  @com.google.common.annotations.VisibleForTesting
  class ApplicationState {
    /** An internal map of all known rule types.  */
    val stringToKind: MutableMap<String, Kind> =
      Collections.synchronizedMap(HashMap())

    /** An internal map of all known rule types.  */
    val perLanguageKinds: Multimap<LanguageClass, Kind> =
      Multimaps.synchronizedSetMultimap<LanguageClass, Kind>(HashMultimap.create())

    init {
      // initialize the global state
      Provider.EP_NAME.extensions
        .flatMap { it.targetKinds }
        .forEach { this.cacheIfNecessary(it) }
    }

    /** Add the Kind instance to the global map, or returns an existing kind with this rule name.  */
    fun cacheIfNecessary(kind: Kind): Kind {
      val existing = stringToKind.putIfAbsent(kind.kindString, kind)
      if (existing != null) {
        return existing
      }
      kind.languageClasses.forEach(
        Consumer { languageClass: LanguageClass? ->
          perLanguageKinds.put(
            languageClass,
            kind,
          )
        },
      )
      return kind
    }

    companion object {
      val service: ApplicationState
        get() =
          ApplicationManager
            .getApplication()
            .getService<ApplicationState>(ApplicationState::class.java)
    }
  }

  fun isOneOf(vararg kinds: Kind): Boolean = kinds.contains(this)

  val isWebTest: Boolean
    get() = this.ruleType == RuleType.TEST && this.kindString.endsWith("web_test")

  fun hasLanguage(languageClass: LanguageClass): Boolean = this.languageClasses.contains(languageClass)

  fun hasAnyLanguageIn(vararg languageClass: LanguageClass): Boolean = languageClass.any { this.languageClasses.contains(it) }

  override fun toString(): String = this.kindString

  companion object {
    /**
     * When a rule has a transition applied to it then it will present with this
     * prefix on it.
     */
    private const val RULE_NAME_PREFIX_TRANSITION = "_transition_"

    fun fromProto(proto: BspTargetInfo.TargetInfo): Kind? {
      val existing = fromRuleName(proto.getKind())
      if (existing != null) {
        return existing
      }
      // check provided heuristics
      var derived = Provider.EP_NAME.extensions.firstNotNullOfOrNull { it.targetKindHeuristics.apply(proto) }
      if (derived != null) {
        derived = ApplicationState.service.cacheIfNecessary(derived)
      }
      return derived
    }

    fun fromRuleName(ruleName: String): Kind? {
      var ruleName = ruleName
      if (ruleName.startsWith(RULE_NAME_PREFIX_TRANSITION)) {
        ruleName = ruleName.substring(RULE_NAME_PREFIX_TRANSITION.length)
      }
      return ApplicationState.service.stringToKind[ruleName]
    }

    val perLanguageKinds: Multimap<LanguageClass, Kind>
      /**
       * Returns a per-language map of rule kinds handled by an available [Provider].
       *
       *
       * Don't rely on this map being complete -- some rule names are recognized at runtime using
       * heuristics.
       */
      get() {
        val state =
          ApplicationState.service
        synchronized(state.perLanguageKinds) {
          return state.perLanguageKinds
        }
      }

    fun getKindsForLanguage(language: LanguageClass): Set<Kind> = perLanguageKinds.get(language).toSet()

    /** If rule type isn't recognized, uses a heuristic to guess the rule type.  */
    fun guessRuleType(ruleName: String): RuleType? {
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
  }
}
