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
package org.jetbrains.bazel.run2.state

import javax.swing.JComponent

/** Provides support for editing [RunConfigurationState]s.  */
interface RunConfigurationStateEditor {
  /** Reset the editor based on the given state.  */
  fun resetEditorFrom(state: RunConfigurationState)

  /** Update the given state based on the editor.  */
  fun applyEditorTo(state: RunConfigurationState)

  /** @return A component to display for the editor.
   */
  fun createComponent(): JComponent

  fun setComponentEnabled(enabled: Boolean)
}
