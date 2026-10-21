package org.jetbrains.bazel.languages.bazelrc

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.BazelLanguagesBundle
import org.jetbrains.bazel.languages.BazelLanguagesIcons
import javax.swing.Icon

@ApiStatus.Internal
object BazelrcLanguage : Language("Bazelrc")

@ApiStatus.Internal
object BazelrcFileType : LanguageFileType(BazelrcLanguage) {
  override fun getName(): String = "Bazelrc"

  override fun getDescription(): String = BazelLanguagesBundle.message("bazelrc.language.description")

  override fun getDefaultExtension(): String = "bazelrc"

  override fun getIcon(): Icon = BazelLanguagesIcons.bazel
}
