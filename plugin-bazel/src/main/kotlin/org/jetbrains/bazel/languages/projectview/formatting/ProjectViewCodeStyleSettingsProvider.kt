package org.jetbrains.bazel.languages.projectview.formatting

import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

class ProjectViewCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
  override fun getCodeSample(settingsType: SettingsType): String? {
    return null
  }

  override fun customizeDefaults(commonSettings: CommonCodeStyleSettings, indentOptions: CommonCodeStyleSettings.IndentOptions) {
    commonSettings.LINE_COMMENT_AT_FIRST_COLUMN = false
    commonSettings.LINE_COMMENT_ADD_SPACE = true
    indentOptions.TAB_SIZE = 2
    indentOptions.INDENT_SIZE = 2
    indentOptions.CONTINUATION_INDENT_SIZE = 2
  }

  override fun getLanguage(): Language {
    return ProjectViewLanguage
  }

  override fun getLanguageName(): String {
    return "ProjectView"
  }

  override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings? {
    return ProjectViewCodeStyleSettings(settings)
  }
}
