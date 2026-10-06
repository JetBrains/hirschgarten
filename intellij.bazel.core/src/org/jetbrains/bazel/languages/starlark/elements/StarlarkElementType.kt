package org.jetbrains.bazel.languages.starlark.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage

@ApiStatus.Internal
class StarlarkElementType(debugName: String) : IElementType(debugName, StarlarkLanguage)
