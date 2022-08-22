package org.jetbrains.bsp.bazel.languages.starlark

import com.intellij.psi.tree.IElementType

class StarlarkTokenType(debugName: String) : IElementType(debugName, StarlarkLanguage)

class StarlarkElementType(debugName: String) : IElementType(debugName, StarlarkLanguage)
