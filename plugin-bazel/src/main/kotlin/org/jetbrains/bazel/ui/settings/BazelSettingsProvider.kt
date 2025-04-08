package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel

interface BazelSettingsProvider {
  fun addGeneralSettings(): Panel.() -> Unit

  fun addExperimentalSettings(): Panel.() -> Unit

  fun isModified(): Boolean

  fun apply()

  companion object {
    val ep = ExtensionPointName.create<BazelSettingsProvider>("org.jetbrains.bazel.bazelSettingsProvider")
  }
}

val Project.bazelSettingsProvider: List<BazelSettingsProvider>
  get() = BazelSettingsProvider.ep.getExtensions(this).toList()
