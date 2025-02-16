package org.jetbrains.bazel.languages.projectview.formatting

import com.intellij.application.options.CodeStyleGenerationConfigurable
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.DisplayPriority
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

class ProjectViewGenerationSettingsProvider : CodeStyleSettingsProvider() {
  override fun createConfigurable(settings: CodeStyleSettings, originalSettings: CodeStyleSettings): CodeStyleConfigurable =
    CodeStyleGenerationConfigurable(settings)

  override fun getConfigurableDisplayName(): String = ApplicationBundle.message("title.code.generation", *arrayOfNulls<Any>(0))

  override fun getPriority(): DisplayPriority = DisplayPriority.CODE_SETTINGS

  override fun hasSettingsPage(): Boolean = false

  override fun getLanguage(): Language? = ProjectViewLanguage
}
