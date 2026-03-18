package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project

internal interface BazelSettingsProvider {
  fun createConfigurable(project: Project): UnnamedConfigurable

  fun searchIndexKeys(): List<String>
}

internal object BazelExperimentalSettingsProvider {
  val ep = ExtensionPointName.create<BazelSettingsProvider>("org.jetbrains.bazel.bazelExperimentalSettingsProvider")

  fun searchIndexKeys(): List<String> = ep.extensionList.flatMap { it.searchIndexKeys() }
}

internal object BazelGeneralSettingsProvider {
  val ep = ExtensionPointName.create<BazelSettingsProvider>("org.jetbrains.bazel.bazelGeneralSettingsProvider")

  fun searchIndexKeys(): List<String> = ep.extensionList.flatMap { it.searchIndexKeys() }
}

internal val Project.bazelExperimentalSettingsProviders: List<BazelSettingsProvider>
  get() = BazelExperimentalSettingsProvider.ep.extensionList
internal val Project.bazelGeneralSettingsProviders: List<BazelSettingsProvider>
  get() = BazelGeneralSettingsProvider.ep.extensionList
