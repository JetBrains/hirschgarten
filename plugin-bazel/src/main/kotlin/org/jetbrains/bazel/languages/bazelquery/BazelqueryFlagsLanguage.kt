package org.jetbrains.bazel.languages.bazelquery

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.bazel.assets.BazelPluginIcons
import javax.swing.Icon

object BazelqueryFlagsLanguage : Language("BazelqueryFlags")




object BazelqueryFlagsFileType : LanguageFileType(BazelqueryFlagsLanguage) {
  override fun getName(): String = "BazelqueryFlags"

  override fun getDescription(): String = "Bazelquery Flags language"

  override fun getDefaultExtension(): String = "org/jetbrains/bazel/languages/bazelquery"

  override fun getIcon(): Icon = BazelPluginIcons.bazel
}
