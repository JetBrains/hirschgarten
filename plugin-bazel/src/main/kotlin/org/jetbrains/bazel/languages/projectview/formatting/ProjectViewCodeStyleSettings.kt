package org.jetbrains.bazel.languages.projectview.formatting

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

internal class ProjectViewCodeStyleSettings(settings: CodeStyleSettings) : CustomCodeStyleSettings("ProjectView", settings)
