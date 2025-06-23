package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContext

interface PythonSynonymProvider {
  fun getSynonym(name: QualifiedName, context: PyQualifiedNameResolveContext): QualifiedName?

  companion object {
    val ep = ExtensionPointName.create<PythonSynonymProvider>("org.jetbrains.bazel.python.synonymProvider")
  }
}
