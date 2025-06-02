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
package org.jetbrains.bazel.run2

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label

/** A factory creating run configurations based on Blaze targets.  */
abstract class BlazeRunConfigurationFactory {
  /** Returns whether this factory can handle a target.  */
  abstract fun handlesTarget(
    project: Project,
    blazeProjectData: BlazeProjectData,
    label: Label,
  ): Boolean

  /** Returns whether this factory is compatible with the given run configuration type.  */
  fun handlesConfiguration(configuration: RunConfiguration): Boolean = this.configurationFactory.type == configuration.getType()

  /** Constructs and initializes [RunnerAndConfigurationSettings] for the given rule.  */
  fun createForTarget(
    project: Project,
    runManager: RunManager,
    target: Label,
  ): RunnerAndConfigurationSettings {
    val factory = this.configurationFactory
    val configuration = factory.createTemplateConfiguration(project, runManager)
    setupConfiguration(configuration, target)
    return runManager.createConfiguration(configuration, factory)
  }

  /** The factory used to create configurations.  */
  protected abstract val configurationFactory: ConfigurationFactory

  /** Initialize the configuration for the given target.  */
  abstract fun setupConfiguration(configuration: RunConfiguration, target: Label)

  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<BlazeRunConfigurationFactory> =
      ExtensionPointName.create("com.google.idea.blaze.RunConfigurationFactory")
  }
}
