package org.jetbrains.bsp.bazel.languages.starlark.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.jetbrains.bsp.bazel.languages.starlark.psi.StarlarkParameterList

class StarlarkParameterListImpl(node: ASTNode) : ASTWrapperPsiElement(node), StarlarkParameterList
