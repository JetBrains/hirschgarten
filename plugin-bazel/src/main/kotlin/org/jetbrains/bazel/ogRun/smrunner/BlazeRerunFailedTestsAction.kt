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

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.TestFrameworkRunningModel
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentContainer
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.bazel.ogRun.BlazeCommandRunConfiguration
import org.jetbrains.bazel.ogRun.other.BlazeCommandName
import org.jetbrains.bazel.ogRun.state.BlazeCommandRunConfigurationCommonState
import java.util.*
import java.util.stream.Collectors

/** Re-run failed tests.  */
class BlazeRerunFailedTestsAction internal constructor(
  private val eventsHandler: BlazeTestEventsHandler,
  componentContainer: ComponentContainer,
) : AbstractRerunFailedTestsAction(componentContainer) {
  override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile? {
    val model: TestFrameworkRunningModel = getModel() ?: return null
    val config: BlazeCommandRunConfiguration =
      model.properties.configuration as BlazeCommandRunConfiguration
    return BlazeRerunTestRunProfile(config.clone())
  }

  private inner class BlazeRerunTestRunProfile(private val configuration: BlazeCommandRunConfiguration) :
    MyRunProfile(configuration) {

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
      val handlerState: BlazeCommandRunConfigurationCommonState? =
        configuration.getHandlerStateIfType(
          BlazeCommandRunConfigurationCommonState::class.java,
        )
      if (handlerState == null ||
        BlazeCommandName.TEST != handlerState.commandState.getCommand()
      ) {
        return null
      }
      val project = project
      val locations = getFailedTests(project)
        .filter { it?.isLeaf == true }
        .mapNotNull { toLocation(project, it!!) }
      val testFilter = eventsHandler.getTestFilter(project, locations) ?: return null
      val blazeFlags =
        setTestFilter(handlerState.blazeFlagsState.rawFlags, testFilter)
      handlerState.blazeFlagsState.rawFlags = blazeFlags
      return configuration.getState(executor, environment)
    }

    private fun toLocation(project: Project, test: AbstractTestProxy): Location<*>? =
      test.getLocation(project, GlobalSearchScope.allScope(project))

    /** Replaces existing test_filter flag, or appends if none exists.  */
    private fun setTestFilter(flags: MutableList<String?>, testFilter: String?): MutableList<String> {
      val copy: MutableList<String> = ArrayList<String>(flags)
      for (i in copy.indices) {
        val flag = copy.get(i)
        if (flag.startsWith(BlazeFlags.TEST_FILTER)) {
          copy.set(i, testFilter!!)
          return copy
        }
      }
      copy.add(testFilter!!)
      return copy
    }
  }
}
