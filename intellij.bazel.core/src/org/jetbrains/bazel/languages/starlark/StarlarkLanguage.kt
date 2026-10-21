package org.jetbrains.bazel.languages.starlark

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.BazelLanguagesBundle
import org.jetbrains.bazel.languages.BazelLanguagesIcons
import javax.swing.Icon

@ApiStatus.Internal
object StarlarkLanguage : Language("Starlark")

@ApiStatus.Internal
object StarlarkFileType : LanguageFileType(StarlarkLanguage) {
  override fun getName(): String = "Starlark"

  override fun getDescription(): String = BazelLanguagesBundle.message("starlark.language.description")

  override fun getDefaultExtension(): String = "bzl"

  override fun getIcon(): Icon = BazelLanguagesIcons.bazel
}
