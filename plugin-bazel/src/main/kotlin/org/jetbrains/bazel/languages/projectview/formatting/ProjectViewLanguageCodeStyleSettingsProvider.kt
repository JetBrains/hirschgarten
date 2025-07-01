package org.jetbrains.bazel.languages.projectview.formatting

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

class ProjectViewLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
  override fun getLanguage(): Language = ProjectViewLanguage

  override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

  override fun getCodeSample(settingsType: SettingsType): String? =
    """
    targets:
      //java/com/google/android/myproject:MyProjectDevTarget
      -//java/com/google/android/myproject:MyExpensiveTarget
      //javatests/com/google/android/myproject/...
    """.trimIndent()

  override fun customizeDefaults(commonSettings: CommonCodeStyleSettings, indentOptions: CommonCodeStyleSettings.IndentOptions) {
    commonSettings.LINE_COMMENT_AT_FIRST_COLUMN = false
    commonSettings.LINE_COMMENT_ADD_SPACE = true
    indentOptions.TAB_SIZE = 2
    indentOptions.INDENT_SIZE = 2
    indentOptions.CONTINUATION_INDENT_SIZE = 2
  }

  override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings = ProjectViewCodeStyleSettings(settings)

  override fun getConfigurableDisplayName() = BazelPluginBundle.message("bazel.language.codestyle.display.name")

  override fun createConfigurable(settings: CodeStyleSettings, modelSettings: CodeStyleSettings): CodeStyleConfigurable =
    object : CodeStyleAbstractConfigurable(settings, modelSettings, this.configurableDisplayName) {
      override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
        ProjectViewCodeStyleMainPanel(currentSettings, settings)
    }

  private class ProjectViewCodeStyleMainPanel(currentSettings: CodeStyleSettings?, settings: CodeStyleSettings) :
    TabbedLanguageCodeStylePanel(ProjectViewLanguage, currentSettings, settings) {
    override fun initTabs(settings: CodeStyleSettings?) {
      addIndentOptionsTab(settings)
    }
  }
}
