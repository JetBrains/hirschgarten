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
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

internal class StarlarkLoadsIndexExtension : FileBasedIndexExtension<String, Collection<String>>() {

  override fun getName(): ID<String, Collection<String>> = StarlarkLoadsIndex.NAME

  override fun dependsOnFileContent(): Boolean = true

  override fun getKeyDescriptor(): EnumeratorStringDescriptor = EnumeratorStringDescriptor.INSTANCE

  override fun getValueExternalizer(): CollectionDataExternalizer<String> = CollectionDataExternalizer(EnumeratorStringDescriptor.INSTANCE)

  override fun getInputFilter(): DefaultFileTypeSpecificInputFilter = DefaultFileTypeSpecificInputFilter(StarlarkFileType)

  override fun getVersion(): Int = 1

  override fun getIndexer(): DataIndexer<String, Collection<String>, FileContent> = DataIndexer { fileContent ->
    val indexes = mutableMapOf<String, MutableList<String>>()
    val file = fileContent.psiFile
    val loadStatements = file.childrenOfType<StarlarkLoadStatement>()
    for (loadStatement in loadStatements) {
      val loadFileName = loadStatement.getLoadedFileNamePsi()?.getStringContents() ?: continue
      for (loadValue in loadStatement.getLoadedSymbolsPsi()) {
        val symbolName = when (loadValue) {
          is StarlarkStringLoadValue, is StarlarkNamedLoadValue -> loadValue.getLoadValueExpressionContent()
          else -> null
        } ?: continue
        val loadFileNames = indexes.getOrPut(symbolName, ::mutableListOf)
        loadFileNames.add(loadFileName)
      }
    }
    return@DataIndexer indexes
  }
}
