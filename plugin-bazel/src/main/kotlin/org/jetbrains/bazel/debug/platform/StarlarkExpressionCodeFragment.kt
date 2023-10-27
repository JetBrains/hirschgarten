package org.jetbrains.bazel.debug.platform

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile

class StarlarkExpressionCodeFragment(
  project: Project,
  extensionlessName: String,
  content: String,
  private val context: PsiElement?,
) : StarlarkFile(
  PsiManagerEx.getInstanceEx(project).fileManager.createFileViewProvider(
    LightVirtualFile(extensionlessName.withExtension(), StarlarkFileType, content),
    true
  )
) {
  override fun getContext(): PsiElement? = if (context?.isValid == true) context else super.getContext()
}

private fun String.withExtension(): String =
  "$this.${StarlarkFileType.defaultExtension}"
