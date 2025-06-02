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

import com.google.idea.blaze.base.command.BlazeFlags
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentContainer
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.bazel.commons.command.BlazeCommandName
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration
import org.jetbrains.bazel.run2.state.BlazeCommandRunConfigurationCommonState
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.collections.filter
import kotlin.collections.indices
import kotlin.sequences.filter
import kotlin.text.equals
import kotlin.text.filter
import kotlin.text.startsWith

/** Re-run failed tests.  */
class BlazeRerunFailedTestsAction internal constructor(
  private val eventsHandler: BlazeTestEventsHandler,
  componentContainer: ComponentContainer,
) : AbstractRerunFailedTestsAction(componentContainer) {
  override fun getRunProfile(environment: ExecutionEnvironment): AbstractRerunFailedTestsAction.MyRunProfile? {
    val model = getModel() ?: return null
    val config: BlazeCommandRunConfiguration =
      model.properties.configuration as BlazeCommandRunConfiguration
    return BlazeRerunTestRunProfile(config.clone())
  }

  internal inner class BlazeRerunTestRunProfile(private val configuration: BlazeCommandRunConfiguration) :
    AbstractRerunFailedTestsAction.MyRunProfile(configuration) {
    val modules: Array<Module> = Module.EMPTY_ARRAY

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
      val handlerState: BlazeCommandRunConfigurationCommonState? =
        configuration.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState>(
          BlazeCommandRunConfigurationCommonState::class.java,
        )
      if (handlerState == null ||
        BlazeCommandName.TEST != handlerState.commandState.command
      ) {
        return null
      }
      val project = getProject()
      val locations =
        getFailedTests(project)
          .filter { it.isLeaf }
          .mapNotNull { toLocation(project, it) }
      val testFilter = eventsHandler.getTestFilter(getProject(), locations) ?: return null
      val blazeFlags =
        setTestFilter(handlerState.blazeFlagsState.rawFlags, testFilter)
      handlerState.blazeFlagsState.rawFlags = blazeFlags
      return configuration.getState(executor, environment)
    }

    private fun toLocation(project: Project, test: AbstractTestProxy): Location<*>? =
      test.getLocation(project, GlobalSearchScope.allScope(project))

    /** Replaces existing test_filter flag, or appends if none exists.  */
    private fun setTestFilter(flags: List<String>, testFilter: String): List<String> {
      val copy: MutableList<String> = ArrayList(flags)
      for (i in copy.indices) {
        val flag = copy[i]
        if (flag.startsWith(BlazeFlags.TEST_FILTER)) {
          copy[i] = testFilter
          return copy
        }
      }
      copy.add(testFilter)
      return copy
    }
  }
}
