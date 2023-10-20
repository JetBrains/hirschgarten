package org.jetbrains.bazel.languages.starlark.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage

class StarlarkElementType(debugName: String) : IElementType(debugName, StarlarkLanguage)
