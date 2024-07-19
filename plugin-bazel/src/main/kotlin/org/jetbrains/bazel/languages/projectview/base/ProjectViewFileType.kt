package org.jetbrains.bazel.languages.projectview.base

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon
import org.jetbrains.bazel.languages.starlark.StarlarkLanguageIcons

object ProjectViewFileType : LanguageFileType(ProjectViewLanguage) {
  override fun getName(): String = "ProjectView"

  override fun getDescription(): String = "ProjectView file for Bazel project"

  override fun getDefaultExtension(): String = "bazelproject"

  override fun getIcon(): Icon = StarlarkLanguageIcons.bazel
}
