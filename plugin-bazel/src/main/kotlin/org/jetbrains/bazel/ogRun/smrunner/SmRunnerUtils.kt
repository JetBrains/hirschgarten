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
package org.jetbrains.bazel.ogRun.smrunner

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerTestTreeView
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Getter
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.bazel.ogRun.BlazeCommandRunConfiguration
import java.util.Arrays
import java.util.Objects
import java.util.stream.Collectors
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

/** Utility methods for setting up the SM runner test UI.  */
object SmRunnerUtils {
  const val GENERIC_SUITE_PROTOCOL: String = "blaze:suite"
  const val GENERIC_TEST_PROTOCOL: String = "blaze:test"
  const val TEST_NAME_PARTS_SPLITTER: String = "::"
  const val BLAZE_FRAMEWORK: String = "blaze-test"

  @JvmStatic
  fun getConsoleView(
    project: Project,
    configuration: BlazeCommandRunConfiguration,
    executor: Executor,
    testUiSession: BlazeTestUiSession,
  ): SMTRunnerConsoleView {
    val properties: SMTRunnerConsoleProperties =
      BlazeTestConsoleProperties(configuration, executor, testUiSession)
    val console =
      SMTestRunnerConnectionUtil.createConsole(BLAZE_FRAMEWORK, properties) as SMTRunnerConsoleView
    Disposer.register(project, console)
    console
      .getResultsViewer()
      .getTreeView()!!
      .getSelectionModel()
      .setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
    return console
  }

  fun createRerunFailedTestsAction(result: DefaultExecutionResult): AbstractRerunFailedTestsAction? {
    val console = result.getExecutionConsole()
    if (console !is SMTRunnerConsoleView) {
      return null
    }
    val smConsole = console
    val consoleProperties = smConsole.getProperties()
    if (consoleProperties !is BlazeTestConsoleProperties) {
      return null
    }
    val properties = consoleProperties
    val action = properties.createRerunFailedTestsAction(smConsole)
    if (action != null) {
      action.init(properties)
      action.setModelProvider(Getter { smConsole.getResultsViewer() })
    }
    return action
  }

  @JvmStatic
  fun attachRerunFailedTestsAction(result: DefaultExecutionResult): DefaultExecutionResult {
    val action = createRerunFailedTestsAction(result)
    if (action != null) {
      result.setRestartActions(action)
    }
    return result
  }

  @JvmStatic
  fun getSelectedSmRunnerTreeElements(context: ConfigurationContext): MutableList<Location<*>?> {
    val project = context.project
    val tests = getSelectedTestProxies(context)
    return tests
      .stream()
      .map { test: SMTestProxy? -> test!!.getLocation(project, GlobalSearchScope.allScope(project)) }
      .filter { obj: Location<Any?>? -> Objects.nonNull(obj) }
      .collect(Collectors.toList())
  }

  /** Counts all selected test cases, and their children, recursively  */
  @JvmStatic
  fun countSelectedTestCases(context: ConfigurationContext): Int {
    val tests = getSelectedTestProxies(context)
    val allTests: MutableSet<SMTestProxy?> = HashSet<SMTestProxy?>(tests)
    for (test in tests) {
      allTests.addAll(test.collectChildren())
    }
    return allTests.size
  }

  private fun getSelectedTestProxies(context: ConfigurationContext): MutableList<SMTestProxy> {
    val treeView =
      SMTRunnerTestTreeView.SM_TEST_RUNNER_VIEW.getData(context.getDataContext())
    if (treeView == null) {
      return listOf<SMTestProxy?>()
    }
    val paths = treeView.getSelectionPaths()
    if (paths == null || paths.size == 0) {
      return listOf<SMTestProxy?>()
    }
    return Arrays
      .stream<TreePath?>(paths)
      .map<SMTestProxy?> { path: TreePath? -> SmRunnerUtils.toTestProxy(treeView, path!!) }
      .filter { obj: SMTestProxy? -> Objects.nonNull(obj) }
      .collect(Collectors.toList())
  }

  private fun toTestProxy(treeView: SMTRunnerTestTreeView, path: TreePath): SMTestProxy? {
    if (treeView.isPathSelected(path.getParentPath())) {
      return null
    }
    return treeView.getSelectedTest(path)
  }
}
