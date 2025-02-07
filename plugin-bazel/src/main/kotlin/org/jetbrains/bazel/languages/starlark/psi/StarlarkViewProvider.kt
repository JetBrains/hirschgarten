package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage

class StarlarkViewProvider(
  manager: PsiManager,
  virtualFile: VirtualFile,
  eventSystemEnabled: Boolean,
  language: Language,
) : SingleRootFileViewProvider(manager, virtualFile, eventSystemEnabled, language) {
  // Make sure to create Starlark PSI even for excluded directories (such as bazel-out)
  override fun shouldCreatePsi() = baseLanguage is StarlarkLanguage

  override fun createCopy(copy: VirtualFile): SingleRootFileViewProvider = StarlarkViewProvider(manager, copy, false, baseLanguage)
}

class StarlarkViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(
    file: VirtualFile,
    language: Language,
    manager: PsiManager,
    eventSystemEnabled: Boolean,
  ) = StarlarkViewProvider(manager, file, eventSystemEnabled, language)
}
