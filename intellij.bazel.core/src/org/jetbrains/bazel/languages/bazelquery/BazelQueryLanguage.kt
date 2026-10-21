package org.jetbrains.bazel.languages.bazelquery

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.BazelLanguagesBundle
import org.jetbrains.bazel.languages.BazelLanguagesIcons
import javax.swing.Icon

@ApiStatus.Internal
object BazelQueryLanguage : Language("BazelQuery")

internal object BazelQueryFileType : LanguageFileType(BazelQueryLanguage) {
  override fun getName(): String = "BazelQuery"

  override fun getDescription(): String = BazelLanguagesBundle.message("bazelquery.language.description")

  override fun getDefaultExtension(): String = ".bazelquery"

  override fun getIcon(): Icon = BazelLanguagesIcons.bazel
}
