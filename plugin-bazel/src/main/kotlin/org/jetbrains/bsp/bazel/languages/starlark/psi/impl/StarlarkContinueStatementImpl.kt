package org.jetbrains.bsp.bazel.languages.starlark.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.jetbrains.bsp.bazel.languages.starlark.psi.StarlarkContinueStatement

class StarlarkContinueStatementImpl(node: ASTNode) : ASTWrapperPsiElement(node), StarlarkContinueStatement
