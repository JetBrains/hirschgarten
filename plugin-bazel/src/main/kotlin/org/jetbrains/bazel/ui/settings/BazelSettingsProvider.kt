package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project

interface BazelSettingsProvider {
  fun createConfigurable(project: Project): UnnamedConfigurable

  fun searchIndexKeys(): List<String>
}

object BazelExperimentalSettingsProvider {
  val ep = ExtensionPointName.create<BazelSettingsProvider>("org.jetbrains.bazel.bazelExperimentalSettingsProvider")

  fun searchIndexKeys(): List<String> = ep.extensionList.flatMap { it.searchIndexKeys() }
}

object BazelGeneralSettingsProvider {
  val ep = ExtensionPointName.create<BazelSettingsProvider>("org.jetbrains.bazel.bazelGeneralSettingsProvider")
}

val Project.bazelExperimentalSettingsProviders: List<BazelSettingsProvider>
  get() = BazelExperimentalSettingsProvider.ep.extensionList
val Project.bazelGeneralSettingsProviders: List<BazelSettingsProvider>
  get() = BazelGeneralSettingsProvider.ep.extensionList
