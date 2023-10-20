package org.jetbrains.bazel.languages.starlark.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceExpression

class StarlarkReferenceExpressionImpl(node: ASTNode) : ASTWrapperPsiElement(node), StarlarkReferenceExpression
