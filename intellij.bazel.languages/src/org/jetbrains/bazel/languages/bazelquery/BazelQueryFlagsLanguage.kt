package org.jetbrains.bazel.languages.bazelquery

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.bazel.languages.BazelLanguagesBundle
import org.jetbrains.bazel.languages.BazelLanguagesIcons
import javax.swing.Icon

internal object BazelQueryFlagsLanguage : Language("BazelQueryFlags")

internal object BazelQueryFlagsFileType : LanguageFileType(BazelQueryFlagsLanguage) {
  override fun getName(): String = "BazelQueryFlags"

  override fun getDescription(): String = BazelLanguagesBundle.message("bazelquery.flags.language.description")

  override fun getDefaultExtension(): String = ".bazelqueryflags"

  override fun getIcon(): Icon = BazelLanguagesIcons.bazel
}
