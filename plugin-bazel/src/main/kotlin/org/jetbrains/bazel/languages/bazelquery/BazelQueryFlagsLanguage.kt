package org.jetbrains.bazel.languages.bazelquery

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.bazel.assets.BazelPluginIcons
import javax.swing.Icon

object BazelQueryFlagsLanguage : Language("BazelQueryFlags")

object BazelQueryFlagsFileType : LanguageFileType(BazelQueryFlagsLanguage) {
  override fun getName(): String = "BazelQueryFlags"

  override fun getDescription(): String = "BazelQuery Flags language"

  override fun getDefaultExtension(): String = ".bazelqueryflags"

  override fun getIcon(): Icon = BazelPluginIcons.bazel
}
