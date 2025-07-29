package org.jetbrains.bazel.languages.starlark.bazel

import com.intellij.openapi.extensions.ExtensionPointName

interface StarlarkClassParametersProvider {
  companion object {
    val EP_NAME: ExtensionPointName<StarlarkClassParametersProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.starlarkClassParametersProvider")
  }

  fun getClassnameParameters(): List<String>
}

class DefaultStarlarkClassParametersProvider : StarlarkClassParametersProvider {
  override fun getClassnameParameters(): List<String> =
    listOf(
      "classname",
      "main_class",
      "test_class",
      "processor_class",
      "genclass",
      "tag_class",
    )
}
