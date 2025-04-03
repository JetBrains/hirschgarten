package org.jetbrains.bazel.languages.bazelquery

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.bazel.assets.BazelPluginIcons
import javax.swing.Icon

object BazelqueryLanguage : Language("Bazelquery")

object BazelqueryFileType : LanguageFileType(BazelqueryLanguage) {
  override fun getName(): String = "Bazelquery"

  override fun getDescription(): String = "Bazelquery language"

  override fun getDefaultExtension(): String = ".bazelquery"

  override fun getIcon(): Icon = BazelPluginIcons.bazel
}
