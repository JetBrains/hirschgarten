package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

internal object NameUtils {
  private val replacementRegex = "[^0-9a-zA-Z]".toRegex()
  fun escape(label: String): String = replacementRegex.replace(label, "_")
}
