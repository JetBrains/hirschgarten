package org.jetbrains.bazel.languages.starlark.bazel

import com.intellij.openapi.extensions.ExtensionPointName

interface StarlarkFilesListParametersProvider {
  companion object {
    val EP_NAME: ExtensionPointName<StarlarkFilesListParametersProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.starlarkFilesListParametersProvider")
  }

  fun getFilesListParameters(): List<String>
}

class DefaultStarlarkFilesListParametersProvider : StarlarkFilesListParametersProvider {
  override fun getFilesListParameters(): List<String> =
    listOf(
      "srcs",
      "hdrs",
      "resources",
      "textual_hdrs"
    )
}
