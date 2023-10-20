package org.jetbrains.bazel.languages.starlark.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkStatementList

class StarlarkStatementListImpl(node: ASTNode) : ASTWrapperPsiElement(node), StarlarkStatementList
