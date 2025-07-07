package org.jetbrains.bazel.languages.bazelquery

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import javax.swing.Icon

object BazelQueryLanguage : Language("BazelQuery")

object BazelQueryFileType : LanguageFileType(BazelQueryLanguage) {
  override fun getName(): String = "BazelQuery"

  override fun getDescription(): String = BazelPluginBundle.message("bazelquery.language.description")

  override fun getDefaultExtension(): String = ".bazelquery"

  override fun getIcon(): Icon = BazelPluginIcons.bazel
}
