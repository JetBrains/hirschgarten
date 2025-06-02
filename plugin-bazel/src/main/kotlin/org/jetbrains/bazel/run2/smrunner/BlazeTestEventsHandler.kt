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
package org.jetbrains.bazel.run2.smrunner

import com.google.common.base.Strings
import com.intellij.execution.Location
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.io.URLUtil
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.TargetPattern
import org.jetbrains.bazel.run2.targetfinder.TargetFinder
import java.util.Optional

/** Stateless language-specific handling of SM runner test protocol  */
interface BlazeTestEventsHandler {
  fun handlesKind(kind: TargetKind?): Boolean

  /**
   * A [SMTestLocator] to convert location URLs provided by this event handler to project PSI
   * elements. Returns `null` if no such conversion is available.
   */
  val testLocator: SMTestLocator?

  /**
   * The --test_filter flag passed to blaze to rerun the given tests.
   *
   * @return `null` if no filter can be constructed for these tests
   */
  fun getTestFilter(project: Project, testLocations: List<Location<*>>): String?

  /** Returns `null` if this test events handler doesn't support test filtering.  */
  fun createRerunFailedTestsAction(consoleView: ConsoleView): AbstractRerunFailedTestsAction? =
    BlazeRerunFailedTestsAction(this, consoleView)

  /** Converts the testsuite name in the blaze test XML to a user-friendly format.  */
  fun suiteDisplayName(
    label: Label,
    kind: TargetKind,
    rawName: String,
  ): String = rawName

  /** Converts the testcase name in the blaze test XML to a user-friendly format.  */
  fun testDisplayName(
    label: Label,
    kind: TargetKind,
    rawName: String,
  ): String = rawName

  /** Converts the suite name to a parsable location URL.  */
  fun suiteLocationUrl(
    label: Label,
    kind: TargetKind,
    name: String,
  ): String = SmRunnerUtils.GENERIC_SUITE_PROTOCOL + URLUtil.SCHEME_SEPARATOR + name

  /** Converts the test case and suite names to a parsable location URL.  */
  fun testLocationUrl(
    label: Label,
    kind: TargetKind,
    parentSuite: String,
    name: String,
    className: String?,
  ): String {
    val base = SmRunnerUtils.GENERIC_TEST_PROTOCOL + URLUtil.SCHEME_SEPARATOR
    if (Strings.isNullOrEmpty(className)) {
      return base + name
    }
    return base + className + SmRunnerUtils.TEST_NAME_PARTS_SPLITTER + name
  }

  /** Whether to skip logging a [TestSuite].  */
  fun ignoreSuite(
    label: Label,
    kind: TargetKind,
    suite: BlazeXmlSchema.TestSuite,
  ): Boolean {
    // by default only include innermost 'testsuite' elements
    return !suite.testSuites.isEmpty()
  }

  companion object {
    /**
     * Whether there's a [BlazeTestEventsHandler] applicable to the given target.
     *
     *
     * Test results will still be displayed for unhandled kinds if they're included in a test_suite
     * or multi-target Blaze invocation, where we don't know up front the languages involved.
     */
    fun targetsSupported(project: Project, targets: List<TargetPattern>): Boolean {
      val kind: TargetKind? = getKindForTargets(project, targets)
      return EP_NAME.extensionList.any { it.handlesKind(kind) }
    }

    /**
     * Returns a [BlazeTestEventsHandler] applicable to the given target.
     *
     *
     * If no such handler exists, falls back to returning [BlazeGenericTestEventsHandler].
     * This adds support for test suites / multi-target invocations, which can mix supported and
     * unsupported target kinds.
     */
    @JvmStatic
    fun getHandlerForTargetKindOrFallback(kind: TargetKind): BlazeTestEventsHandler =
      getHandlerForTargetKind(kind) ?: (BlazeGenericTestEventsHandler())

    /**
     * Returns a [BlazeTestEventsHandler] applicable to the given target or [ ][Optional.empty] if no such handler can be found.
     */
    fun getHandlerForTarget(project: Project, target: TargetPattern): BlazeTestEventsHandler? {
      return getHandlerForTargetKind(getKindForTarget(project, target) ?: return null)
    }

    /**
     * Returns a [BlazeTestEventsHandler] applicable to the given targets or [ ][Optional.empty] if no such handler can be found.
     */
    fun getHandlerForTargets(project: Project, targets: List<TargetPattern>): BlazeTestEventsHandler? =
      getHandlerForTargetKind(getKindForTargets(project, targets))

    /**
     * Returns a [BlazeTestEventsHandler] applicable to the given target kind, or [ ][Optional.empty] if no such handler can be found.
     */
    @JvmStatic
    fun getHandlerForTargetKind(kind: TargetKind): BlazeTestEventsHandler? = EP_NAME.extensionList.firstOrNull { it.handlesKind(kind) }

    /** Returns the single TargetKind shared by all targets or null if they have different kinds.  */
    fun getKindForTargets(project: Project, targets: List<TargetPattern>): TargetKind? {
      // TODO(brendandouglas): extend BlazeTestEventsHandler API to handle multiple targets with
      // *known* kinds
      var singleKind: TargetKind? = null
      for (target in targets) {
        val kind: TargetKind? = getKindForTarget(project, target)
        if (kind == null || (singleKind != null && kind != singleKind)) {
          return null
        }
        singleKind = kind
      }
      return singleKind
    }

    fun getKindForTarget(project: Project, target: TargetPattern): TargetKind? {
      if (target !is Label) {
        return null
      }
      val targetInfo: TargetInfo? = TargetFinder.findTargetInfo(project, target)
      return targetInfo?.getKind()
    }

    @JvmField
    val EP_NAME: ExtensionPointName<BlazeTestEventsHandler> =
      ExtensionPointName.create("com.google.idea.blaze.BlazeTestEventsHandler")
  }
}
