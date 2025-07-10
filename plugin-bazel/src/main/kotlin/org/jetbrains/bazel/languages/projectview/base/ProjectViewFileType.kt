package org.jetbrains.bazel.languages.projectview.base

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import javax.swing.Icon

object ProjectViewFileType : LanguageFileType(ProjectViewLanguage) {
  override fun getName(): String = "ProjectView"

  override fun getDescription(): String = BazelPluginBundle.message("bazel.language.projectview.description")

  override fun getDefaultExtension(): String = "bazelproject"

  override fun getIcon(): Icon = BazelPluginIcons.bazel
}
