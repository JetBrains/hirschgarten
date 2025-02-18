package org.jetbrains.bazel.languages.bazelrc.references

import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.psi.util.PsiUtil
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.bazelrc.BazelrcFileType
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcImport

class BazelrcImportReference(
  referenceSet: FileReferenceSet,
  range: TextRange,
  index: Int,
  text: String,
) : FileReference(referenceSet, range, index, text) {
  override fun innerResolve(caseSensitive: Boolean, containingFile: PsiFile): Array<out ResolveResult?> =
    when {
      text == "%workspace%" && index == 0 ->
        arrayOf(PsiUtil.findFileSystemItem(element.project, element.project.rootDir)).filterNotNull()

      else -> {
        val virtualFiles = contexts.mapNotNull { it.virtualFile?.findChild(text) }
        virtualFiles.mapNotNull { PsiUtil.findFileSystemItem(element.project, it) }
      }
    }.let(PsiElementResolveResult::createResults)
}

class BazelrcImportFileReferenceSet(importSpec: BazelrcImport) :
  FileReferenceSet(
    importSpec.getImportPath()?.text ?: "",
    importSpec,
    importSpec.getImportPath()?.startOffsetInParent ?: 0,
    null,
    true,
    true,
    arrayOf(BazelrcFileType),
  ) {
  override fun createFileReference(
    range: TextRange,
    index: Int,
    text: String,
  ): FileReference? = BazelrcImportReference(this, range, index, text)

  override fun computeDefaultContexts(): Collection<PsiFileSystemItem?> = listOf(element.containingFile.originalFile.parent)

  override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem>? =
    Condition {
      (it.isDirectory && !it.name.startsWith(".")) || it.name.endsWith(".bazelrc")
    }
}
