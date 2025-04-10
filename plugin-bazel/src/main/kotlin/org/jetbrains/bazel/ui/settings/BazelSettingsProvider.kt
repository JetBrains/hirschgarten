package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel

interface BazelSettingsProvider {
  /**
   * This function returns a function which will be called in BazelSettingsPanel.createComponent
   * and BazelExperimentalProjectSettingsPanel.createComponent.
   * The returned function should be able to add the new setting ui components into the bazel setting panel.
   * See BazelSettingsPanel.createComponent for more details.
   * */
  fun addGeneralSettings(): Panel.() -> Unit

  /**
   * This function returns a function which will be called in BazelSettingsPanel.createComponent
   * and BazelExperimentalProjectSettingsPanel.createComponent.
   * The returned function should be able to add the new setting ui components
   * into the bazel experimental settings panel.
   * See BazelSettingsPanel.createComponent for more details.
   * */
  fun addExperimentalSettings(): Panel.() -> Unit

  /**
   * This function is called in BazelSettingsPanel.isModified and BazelExperimentalProjectSettingsPanel.isModified.
   * It should be able to indicate whether there are any changes in the settings injected by the provider
   * */
  fun isModified(): Boolean

  /**
   * This function is called in BazelSettingsPanel.apply and BazelExperimentalProjectSettingsPanel.apply
   * It should perform the necessary operations to make the change go into effect(e.g. resync ...)
   * */
  fun apply()

  companion object {
    val ep = ExtensionPointName.create<BazelSettingsProvider>("org.jetbrains.bazel.bazelSettingsProvider")
  }
}

val Project.bazelSettingsProvider: List<BazelSettingsProvider>
  get() = BazelSettingsProvider.ep.getExtensions(this).toList()
