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
package org.jetbrains.bazel.ogRun.state

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.ui.UiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element
import java.util.function.Consumer
import java.util.stream.Collectors

/** Helper class for managing composite state.  */
abstract class RunConfigurationCompositeState : RunConfigurationState {
  protected var states: ImmutableList<RunConfigurationState>? = null
    /** The [RunConfigurationState]s comprising this composite state.  */
    get() {
      if (field == null) {
        field = initializeStates()
      }
      return field
    }
    private set

  /**
   * Called once prior to (de)serializing the run configuration and/or setting up the UI. Returns
   * the [RunConfigurationState]s comprising this composite state. The order of the states
   * determines their position in the UI.
   */
  protected abstract fun initializeStates(): ImmutableList<RunConfigurationState>?

  @Throws(InvalidDataException::class)
  override fun readExternal(element: Element?) {
    for (state in this.states!!) {
      state.readExternal(element)
    }
  }

  /** Updates the element with the handler's state.  */
  @Throws(WriteExternalException::class)
  override fun writeExternal(element: Element?) {
    for (state in this.states!!) {
      state.writeExternal(element)
    }
  }

  /** @return A [RunConfigurationStateEditor] for this state.
   */
  override fun getEditor(project: Project?): RunConfigurationStateEditor = RunConfigurationCompositeStateEditor(project, this.states!!)

  internal class RunConfigurationCompositeStateEditor(project: Project?, states: MutableList<RunConfigurationState?>) :
    RunConfigurationStateEditor {
    var editors: MutableList<RunConfigurationStateEditor?>

    init {
      editors =
        states
          .stream()
          .map<RunConfigurationStateEditor?> { state: RunConfigurationState? -> state!!.getEditor(project) }
          .collect(
            Collectors.toList(),
          )
    }

    override fun resetEditorFrom(genericState: RunConfigurationState?) {
      val state = genericState as RunConfigurationCompositeState
      for (i in editors.indices) {
        editors.get(i)!!.resetEditorFrom(state.states!!.get(i))
      }
    }

    override fun applyEditorTo(genericState: RunConfigurationState?) {
      val state = genericState as RunConfigurationCompositeState
      for (i in editors.indices) {
        editors.get(i)!!.applyEditorTo(state.states!!.get(i))
      }
    }

    override fun createComponent(): JComponent =
      UiUtil.createBox(
        editors
          .stream()
          .map<JComponent?> { obj: RunConfigurationStateEditor? -> obj!!.createComponent() }
          .collect(Collectors.toList()),
      )

    override fun setComponentEnabled(enabled: Boolean) {
      editors.forEach(Consumer { editor: RunConfigurationStateEditor? -> editor!!.setComponentEnabled(enabled) })
    }
  }
}
