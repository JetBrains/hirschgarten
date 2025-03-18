package org.jetbrains.bsp.protocol.og

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import java.util.Arrays
import java.util.Collections
import java.util.Objects
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
data class RuleKind(
  val kindString: String,
  val languageClasses: Set<LanguageClass>,
  val ruleType: RuleType
)
