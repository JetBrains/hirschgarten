package org.jetbrains.bazel.languages.starlark.index

import com.intellij.psi.util.childrenOfType
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.impl.CollectionDataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement

internal class StarlarkLoadEdgesIndexExtension : FileBasedIndexExtension<String, Collection<String>>() {
  override fun getName(): ID<String, Collection<String>> = StarlarkLoadEdgesIndex.NAME

  override fun dependsOnFileContent(): Boolean = true

  override fun getKeyDescriptor(): EnumeratorStringDescriptor = EnumeratorStringDescriptor.INSTANCE

  override fun getValueExternalizer(): CollectionDataExternalizer<String> =
    CollectionDataExternalizer(EnumeratorStringDescriptor.INSTANCE)

  override fun getInputFilter(): DefaultFileTypeSpecificInputFilter =
    DefaultFileTypeSpecificInputFilter(StarlarkFileType)

  override fun getVersion(): Int = 1

  override fun getIndexer(): DataIndexer<String, Collection<String>, FileContent> = DataIndexer { fileContent ->
    val file = fileContent.psiFile
    if (!file.name.endsWith(".bzl")) return@DataIndexer emptyMap()

    val loadLabels = file
      .childrenOfType<StarlarkLoadStatement>()
      .mapNotNull { it.getLoadedFileNamePsi()?.getStringContents() }
      .filter { it.endsWith(".bzl") }
      .distinct()

    if (loadLabels.isEmpty()) emptyMap() else mapOf(fileContent.file.url to loadLabels)
  }
}
