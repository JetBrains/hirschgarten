package org.jetbrains.bazel.languages.projectview.formatting

import com.intellij.application.options.CodeStyleGenerationConfigurable
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.options.Configurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.DisplayPriority
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

class ProjectViewGenerationSettingsProvider : CodeStyleSettingsProvider() {
  @Deprecated("Deprecated in Java")
  override fun createSettingsPage(settings: CodeStyleSettings, originalSettings: CodeStyleSettings): Configurable {
    return CodeStyleGenerationConfigurable(settings)
  }

  override fun getConfigurableDisplayName(): String {
    return ApplicationBundle.message("title.code.generation", *arrayOfNulls<Any>(0))
  }

  override fun getPriority(): DisplayPriority {
    return DisplayPriority.CODE_SETTINGS
  }

  override fun hasSettingsPage(): Boolean {
    return false
  }

  override fun getLanguage(): Language? {
    return ProjectViewLanguage
  }
}
