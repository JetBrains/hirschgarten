package org.jetbrains.bazel.languages.starlark

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import javax.swing.Icon

@ApiStatus.Internal
object StarlarkLanguage : Language("Starlark")

@ApiStatus.Internal
object StarlarkFileType : LanguageFileType(StarlarkLanguage) {
  override fun getName(): String = "Starlark"

  override fun getDescription(): String = BazelPluginBundle.message("starlark.language.description")

  override fun getDefaultExtension(): String = "bzl"

  override fun getIcon(): Icon = BazelPluginIcons.bazel
}
