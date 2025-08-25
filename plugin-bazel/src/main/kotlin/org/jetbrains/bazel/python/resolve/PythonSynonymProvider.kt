package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.extensions.ExtensionPointName

/** Use this extension point if resolving Python imports require a custom change in their qualified names */
interface PythonSynonymProvider {
  /**
   * Creates a synonym for a Python import.
   * Qualified name returned from here (a "synonym") is searched in the project root when the original name was not found.
   * For example, if in Pythin code a library is referenced as `aaa.bbb`, but its file system location is `src/python/aaa/bbb.py`,
   * then the override might look as follows:
   * ```
   * return listOf("src", "python") + qualifiedNameComponents
   * ```
   *
   * If for a given qualified name, no synonym should be provided, return `null`
   */
  fun getSynonym(qualifiedNameComponents: List<String>): List<String>?

  companion object {
    val ep = ExtensionPointName.create<PythonSynonymProvider>("org.jetbrains.bazel.python.synonymProvider")
  }
}
