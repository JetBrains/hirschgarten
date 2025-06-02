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

import com.intellij.execution.Executor
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.ui.ConsoleView
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration

/** Integrates blaze test results with the SM-runner test UI.  */
class BlazeTestConsoleProperties(
  private val runConfiguration: BlazeCommandRunConfiguration,
  executor: Executor,
  private val testUiSession: BlazeTestUiSession,
) : SMTRunnerConsoleProperties(runConfiguration, SmRunnerUtils.BLAZE_FRAMEWORK, executor),
  SMCustomMessagesParsing {
  override fun createTestEventsConverter(framework: String, consoleProperties: TestConsoleProperties): OutputToGeneralTestEventsConverter =
    BlazeXmlToTestEventsConverter(
      framework,
      consoleProperties,
      testUiSession.getTestResultFinderStrategy(),
    )

  override fun getTestLocator(): SMTestLocator? =
    CompositeSMTestLocator(
      BlazeTestEventsHandler.EP_NAME.extensionList
        .mapNotNull(BlazeTestEventsHandler::testLocator),
    )

  override fun createRerunFailedTestsAction(consoleView: ConsoleView): AbstractRerunFailedTestsAction? =
    BlazeTestEventsHandler
      .getHandlerForTargets(
        runConfiguration.project,
        runConfiguration.targets,
      )?.createRerunFailedTestsAction(consoleView)
}
