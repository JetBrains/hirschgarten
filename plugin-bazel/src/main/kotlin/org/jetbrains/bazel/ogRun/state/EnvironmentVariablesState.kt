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
package org.jetbrains.bazel.ogRun.state

import com.google.common.collect.ImmutableMap
import com.google.idea.blaze.base.command.BlazeFlags
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element
import org.jetbrains.annotations.VisibleForTesting
import javax.swing.JComponent

/** State for user-defined environment variables.  */
class EnvironmentVariablesState : RunConfigurationState {
  var data: EnvironmentVariablesData =
    EnvironmentVariablesData.create(ImmutableMap.of<String?, String?>(), /* passParentEnvs= */true)
    private set

  fun asBlazeTestEnvFlags(): List<String> =
    data.envs.map { (key, value) ->
      "${BlazeFlags.TEST_ENV}=$key=$value"
    }

  @Throws(InvalidDataException::class)
  override fun readExternal(element: Element) {
    val child = element.getChild(ELEMENT_TAG)
    if (child != null) {
      data = EnvironmentVariablesData.readExternal(child)
    }
  }

  @Throws(WriteExternalException::class)
  override fun writeExternal(element: Element) {
    element.removeChildren(ELEMENT_TAG)
    val child = Element(ELEMENT_TAG)
    data.writeExternal(child)
    element.addContent(child)
  }

  override fun getEditor(project: Project): RunConfigurationStateEditor = Editor()

  @VisibleForTesting
  fun setEnvVars(vars: Map<String, String>) {
    data = data.with(vars)
  }

  private class Editor : RunConfigurationStateEditor {
    private val component = EnvironmentVariablesComponent()

    init {
      component.setText("&Environment variables")
    }

    override fun resetEditorFrom(genericState: RunConfigurationState) {
      val state = genericState as EnvironmentVariablesState
      component.envs = state.data.envs
      component.isPassParentEnvs = state.data.isPassParentEnvs
    }

    override fun applyEditorTo(genericState: RunConfigurationState) {
      val state = genericState as EnvironmentVariablesState
      state.data =
        EnvironmentVariablesData.create(component.envs, component.isPassParentEnvs)
    }

    override fun createComponent(): JComponent = component

    override fun setComponentEnabled(enabled: Boolean) {
      component.setEnabled(enabled)
    }
  }

  companion object {
    private const val ELEMENT_TAG = "env_state"
  }
}
