package org.jetbrains.bazel.python.lang

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassProvider

@ApiStatus.Internal
object PythonLanguageClass {
  val PYTHON: LanguageClass = LanguageClass("python", setOf("py", "pyw", "pyi"))
}

internal class PythonLanguageClassProvider: LanguageClassProvider {
  override val languages: List<LanguageClass>
    get() = listOf(PythonLanguageClass.PYTHON)
}
