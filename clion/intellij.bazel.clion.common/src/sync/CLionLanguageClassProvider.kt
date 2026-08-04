package org.jetbrains.bazel.clion.sync

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassProvider

internal object CLionLanguageClass {
  val CPP = LanguageClass("cpp", setOf("cc", "cpp", "hh"))
}

internal class CLionLanguageClassProvider: LanguageClassProvider {
  override val languages: List<LanguageClass>
    get() = kotlin.collections.listOf(CLionLanguageClass.CPP)
}
