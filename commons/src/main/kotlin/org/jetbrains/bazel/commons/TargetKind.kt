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

// For compatibility with OG
typealias Kind = TargetKind

/**
 * A set of recognized bazel rule names, together with [LanguageClass] and [RuleType].
 * Language-specific extensions can provide their own set of rule names, as well as heuristics to
 * recognize rule names at runtime.
 *
 *
 * Each rule name maps to at most one Kind.
 */
data class TargetKind(
  val kindString: String,
  val languageClasses: Set<LanguageClass> = emptySet(),
  val ruleType: RuleType,
) {
  // Used in extension functions
  companion object

  val isExecutable: Boolean = ruleType == RuleType.BINARY || ruleType == RuleType.TEST
}
