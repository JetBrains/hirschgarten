package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import org.jetbrains.magicmetamodel.extensions.allSubdirectoriesSequence
import org.jetbrains.magicmetamodel.extensions.toAbsolutePath
import java.net.URI
import java.nio.file.Path
import kotlin.reflect.KProperty

internal class TargetsDetailsForDocumentProvider(sources: List<SourcesItem>) {

  init {
    LOGGER.trace { "Initializing TargetsDetailsForDocumentProvider..." }
  }

  private val documentIdToTargetsIdsMap by DocumentIdToTargetsIdsMapDelegate(sources)

  init {
    LOGGER.trace { "Initializing TargetsDetailsForDocumentProvider done!" }
  }

  fun getAllDocuments(): List<TextDocumentIdentifier> =
    documentIdToTargetsIdsMap.keys
      .map(this::mapPathToTextDocumentIdentifier)
      .toList()

  private fun mapPathToTextDocumentIdentifier(path: Path): TextDocumentIdentifier =
    TextDocumentIdentifier(path.toUri().toString())

  fun getTargetsDetailsForDocument(documentId: TextDocumentIdentifier): List<BuildTargetIdentifier> =
    generateAllDocumentSubdirectoriesIncludingDocument(documentId)
      .flatMap { documentIdToTargetsIdsMap[it].orEmpty() }
      .toList()

  private fun generateAllDocumentSubdirectoriesIncludingDocument(documentId: TextDocumentIdentifier): Sequence<Path> {
    LOGGER.trace { "Generating all $documentId subdirectories..." }

    val documentAbsolutePath = mapDocumentIdToAbsolutePath(documentId)

    return documentAbsolutePath.allSubdirectoriesSequence()
      .also { LOGGER.trace { "Generating all $documentId subdirectories done! Subdirectories: $it." } }
  }

  private fun mapDocumentIdToAbsolutePath(documentId: TextDocumentIdentifier): Path =
    URI.create(documentId.uri).toAbsolutePath()

  companion object {
    private val LOGGER = logger<TargetsDetailsForDocumentProvider>()
  }
}

private class DocumentIdToTargetsIdsMapDelegate(private val sources: List<SourcesItem>) {

  operator fun getValue(
    thisRef: Any?,
    property: KProperty<*>,
  ): Map<Path, Set<BuildTargetIdentifier>> {
    LOGGER.trace { "Calculating document to target id map..." }

    return sources
      .flatMap(this::mapSourcesItemToPairsOfDocumentIdAndTargetId)
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.toSet() }
      .also { LOGGER.trace { "Calculating document to target id map done! Map: $it." } }
  }

  private fun mapSourcesItemToPairsOfDocumentIdAndTargetId(
    sourceItem: SourcesItem,
  ): List<Pair<Path, BuildTargetIdentifier>> =
    sourceItem.sources
      .map(this::mapSourceItemToPath)
      .map { Pair(it, sourceItem.target) }

  private fun mapSourceItemToPath(sourceItem: SourceItem): Path =
    when (sourceItem.kind) {
      SourceItemKind.FILE -> URI.create(sourceItem.uri).toAbsolutePath().parent
      SourceItemKind.DIRECTORY -> URI.create(sourceItem.uri).toAbsolutePath()
      else -> throw TypeCastException("something is really wrong")
    }

  companion object {
    private val LOGGER = logger<DocumentIdToTargetsIdsMapDelegate>()
  }
}
