package org.jetbrains.bazel.languages.bazelversion

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.bazel.assets.BazelPluginIcons
import javax.swing.Icon

object BazelVersionLanguage : Language("BazelVersion")

object BazelVersionFileType : LanguageFileType(BazelVersionLanguage) {
  override fun getName(): String = "BazelVersion"

  override fun getDescription(): String = "BazelVersion language"

  override fun getDefaultExtension(): @NlsSafe String = "bazelversion"

  override fun getIcon(): Icon = BazelPluginIcons.bazel
}
