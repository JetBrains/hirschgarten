/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package org.jetbrains.plugins.bsp.golang.run.smrunner

import com.google.idea.blaze.base.command.BlazeFlags

/** Provides go-specific methods needed by the SM-runner test UI.  */
class BlazeGoTestEventsHandler : BlazeTestEventsHandler {
  public override fun handlesKind(kind: Kind?): Boolean {
    return kind != null && kind.hasLanguage(LanguageClass.GO)
      && kind.getRuleType().equals(RuleType.TEST)
  }

  val testLocator: SMTestLocator
    get() = BlazeGoTestLocator.INSTANCE

  public override fun getTestFilter(
    project: com.intellij.openapi.project.Project?,
    testLocations: MutableList<com.intellij.execution.Location<*>?>
  ): String? {
    val filter: String? =
      testLocations.stream()
        .map { obj: com.intellij.execution.Location<*>? -> obj.getPsiElement() }
        .filter { obj: Any? -> GoFunctionOrMethodDeclaration::class.java.isInstance(obj) }
        .map<GoFunctionOrMethodDeclaration?> { obj: Any? -> GoFunctionOrMethodDeclaration::class.java.cast(obj) }
        .map<String?> { obj: GoFunctionOrMethodDeclaration? -> obj.getName() }
        .distinct()
        .map<String?> { name: String? -> "^" + name + "$" }
        .reduce { a: String?, b: String? -> a + "|" + b }
        .orElse(null)
    return if (filter != null)
      java.lang.String.format(
          "%s=%s", BlazeFlags.TEST_FILTER, BlazeParametersListUtil.encodeParam(filter),
      )
    else
      null
  }

  public override fun suiteDisplayName(label: Label, kind: Kind?, rawName: String?): String {
    return label.toString() // rawName is just the target name, which can be too short
  }

  public override fun suiteLocationUrl(label: Label, kind: Kind?, name: String?): String {
    return (SmRunnerUtils.GENERIC_SUITE_PROTOCOL
      + com.intellij.util.io.URLUtil.SCHEME_SEPARATOR
      + label.withTargetName(name))
  }

  public override fun testLocationUrl(
    label: Label,
    kind: Kind?,
    parentSuite: String?,
    name: String?,
    className: String?
  ): String {
    return (SmRunnerUtils.GENERIC_TEST_PROTOCOL
      + com.intellij.util.io.URLUtil.SCHEME_SEPARATOR
      + label.withTargetName(parentSuite) // target and suite name won't match with web_tests
      + SmRunnerUtils.TEST_NAME_PARTS_SPLITTER
      + name)
  }
}
