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

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind

/** Contributes generic rules to [TargetKind].  */
class GenericBazelRules : TargetKindProvider {
  /** Generic bazel rule types.  */
  enum class RuleTypes(
    val ruleName: String,
    val languageClass: LanguageClass,
    val ruleType: RuleType,
  ) {
    PROTO_LIBRARY("proto_library", LanguageClass.GENERIC, RuleType.LIBRARY),
    TEST_SUITE("test_suite", LanguageClass.GENERIC, RuleType.TEST),
    INTELLIJ_PLUGIN_DEBUG_TARGET(
      "intellij_plugin_debug_target",
      LanguageClass.JAVA,
      RuleType.BINARY,
    ),
    WEB_TEST("web_test", LanguageClass.GENERIC, RuleType.TEST),
    SH_TEST("sh_test", LanguageClass.GENERIC, RuleType.TEST),
    SH_LIBRARY("sh_library", LanguageClass.GENERIC, RuleType.LIBRARY),
    SH_BINARY("sh_binary", LanguageClass.GENERIC, RuleType.BINARY),
    ;

    val kind: TargetKind
      get() = TargetKind.fromRuleName(name)!!
  }

  override val targetKinds: Set<TargetKind>
    get() =
      RuleTypes.entries
        .map { TargetKind(it.name, setOf(it.languageClass), it.ruleType) }
        .toSet()
}
