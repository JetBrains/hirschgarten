package org.jetbrains.bazel.languages.bazelversion

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.BazelLanguagesBundle
import org.jetbrains.bazel.languages.BazelLanguagesIcons
import javax.swing.Icon

internal object BazelVersionLanguage : Language("BazelVersion")

@ApiStatus.Internal
object BazelVersionFileType : LanguageFileType(BazelVersionLanguage) {
  override fun getName(): String = "BazelVersion"

  override fun getDescription(): String = BazelLanguagesBundle.message("bazel.language.description")

  override fun getDefaultExtension(): @NlsSafe String = "bazelversion"

  override fun getIcon(): Icon = BazelLanguagesIcons.bazel
}
