package org.jetbrains.bazel.languages.starlark.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFunctionDeclaration

class StarlarkFunctionDeclarationImpl(node: ASTNode) : ASTWrapperPsiElement(node), StarlarkFunctionDeclaration
