package org.jetbrains.bazel.languages.starlark.formatting.configuration

import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project

class BuildifierConfigurableProvider(val project: Project) : ConfigurableProvider() {
  override fun canCreateConfigurable(): Boolean = true

  override fun createConfigurable(): BuildifierConfigurable = BuildifierConfigurable(project)
}
