package org.jetbrains.bazel.languages.starlark

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.bazel.assets.BazelPluginIcons
import javax.swing.Icon

object StarlarkLanguage : Language("Starlark")

object StarlarkFileType : LanguageFileType(StarlarkLanguage) {
  override fun getName(): String = "Starlark"

  override fun getDescription(): String = "Starlark language"

  override fun getDefaultExtension(): String = "bzl"

  override fun getIcon(): Icon = BazelPluginIcons.bazel
}
