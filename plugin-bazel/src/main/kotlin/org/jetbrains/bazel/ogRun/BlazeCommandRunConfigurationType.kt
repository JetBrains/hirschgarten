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
package org.jetbrains.bazel.ogRun

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.config.isBazelProject
import java.lang.String
import javax.swing.Icon

/** A type for run configurations that execute Blaze commands.  */
class BlazeCommandRunConfigurationType : ConfigurationType, DumbAware {
  val factory: BlazeCommandRunConfigurationFactory = BlazeCommandRunConfigurationType.BlazeCommandRunConfigurationFactory(this)

  /** A run configuration factory  */
  class BlazeCommandRunConfigurationFactory private constructor(type: ConfigurationType) : ConfigurationFactory(type), DumbAware {
    override fun getId(): String {
      // must be left unchanged for backwards compatibility
      return getName()
    }

    override fun isApplicable(project: Project): Boolean =
      project.isBazelProject

    override fun createTemplateConfiguration(project: Project): BlazeCommandRunConfiguration {
      return BlazeCommandRunConfiguration(project, this, "Unnamed")
    }

    // Super method uses raw BeforeRunTask.
    override fun configureBeforeRunTaskDefaults(
      providerID: Key<out BeforeRunTask<*>?>, task: BeforeRunTask<*>
    ) {
      task.setEnabled(providerID == BlazeBeforeRunTaskProvider.ID)
    }
  }

  override fun getDisplayName(): String {
    return "Bazel Command"
  }

  override fun getConfigurationTypeDescription(): String? {
    return String.format(
      "Configuration for launching arbitrary %s commands.", Blaze.guessBuildSystemName(),
    )
  }

  override fun getIcon(): Icon {
    return BlazeIcons.Logo
  }

  override fun getId(): kotlin.String {
    return "BlazeCommandRunConfigurationType"
  }

  override fun getConfigurationFactories(): Array<BlazeCommandRunConfigurationFactory?> {
    return arrayOf<BlazeCommandRunConfigurationFactory>(factory)
  }

  companion object {
    val instance: BlazeCommandRunConfigurationType
      get() = findConfigurationType<BlazeCommandRunConfigurationType?>(BlazeCommandRunConfigurationType::class.java)
  }
}
