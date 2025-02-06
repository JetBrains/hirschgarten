/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package org.jetbrains.plugins.bsp.golang

import com.google.idea.blaze.base.model.primitives.Kind

/** Contributes golang rules to [Kind].  */
class GoBlazeRules : Kind.Provider {
    /** Go-specific blaze rules.  */
    enum class RuleTypes(name: String, languageClass: LanguageClass, ruleType: RuleType) {
        GO_GAZELLE_BINARY("gazelle_binary", LanguageClass.GO, RuleType.BINARY),
        GO_TEST("go_test", LanguageClass.GO, RuleType.TEST),
        GO_APPENGINE_TEST("go_appengine_test", LanguageClass.GO, RuleType.TEST),
        GO_BINARY("go_binary", LanguageClass.GO, RuleType.BINARY),
        GO_APPENGINE_BINARY("go_appengine_binary", LanguageClass.GO, RuleType.BINARY),
        GO_LIBRARY("go_library", LanguageClass.GO, RuleType.LIBRARY),
        GO_APPENGINE_LIBRARY("go_appengine_library", LanguageClass.GO, RuleType.LIBRARY),
        GO_PROTO_LIBRARY("go_proto_library", LanguageClass.GO, RuleType.LIBRARY),
        GO_WRAP_CC("go_wrap_cc", LanguageClass.GO, RuleType.UNKNOWN),
        GO_WEB_TEST("go_web_test", LanguageClass.GO, RuleType.TEST);

        private val name: String?
        private val languageClass: LanguageClass?
        private val ruleType: RuleType?

        init {
            this.name = name
            this.languageClass = languageClass
            this.ruleType = ruleType
        }

        val kind: Kind?
            get() = com.google.common.base.Preconditions.checkNotNull<T?>(Kind.fromRuleName(name))
    }

    val targetKinds: com.google.common.collect.ImmutableSet<Kind?>
        get() = java.util.Arrays.RuleTypes > stream<com.google.idea.blaze.golang.GoBlazeRules.RuleTypes?>(com.google.idea.blaze.golang.GoBlazeRules.RuleTypes.entries.toTypedArray())
            .< Object > map < kotlin . Any ? > ({ e -> Kind.Provider.create(e.name, e.languageClass, e.ruleType) })
            .collect(com.google.common.collect.ImmutableSet.toImmutableSet<Any?>())
}
