package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.util.io.isFile
import org.jetbrains.magicmetamodel.extensions.allSubdirectoriesSequence
import org.jetbrains.magicmetamodel.extensions.toAbsolutePath
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath
import kotlin.reflect.KProperty

internal class TargetsDetailsForDocumentProvider(sources: List<SourcesItem>) {

  init {
    LOGGER.trace { "Initializing TargetsDetailsForDocumentProvider..." }
  }

  private val documentIdToTargetsIdsMap by DocumentIdToTargetsIdsMapDelegate(sources)
  private val documentIdInTheSameDirectoryToTargetsIdsMapForHACK by DocumentIdToTargetsIdsMapDelegateInTheSameDirHACK(
    sources
  )
  private val isFileMap4HACK = sources
    .flatMap { it.sources }
    .associateBy({ it.uri }, { it.kind == SourceItemKind.FILE })

  init {
    LOGGER.trace { "Initializing TargetsDetailsForDocumentProvider done!" }
  }

  fun getAllDocuments(): List<TextDocumentIdentifier> =
    documentIdToTargetsIdsMap.keys
      .map(this::mapPathToTextDocumentIdentifier)
      .toList()

  private fun mapPathToTextDocumentIdentifier(path: Path): TextDocumentIdentifier =
    TextDocumentIdentifier(path.toUri().toString())

  fun getTargetsDetailsForDocument(documentId: TextDocumentIdentifier): List<BuildTargetIdentifier> {
    val targets = generateAllDocumentSubdirectoriesIncludingDocument(documentId)
      .flatMap { documentIdToTargetsIdsMap[it].orEmpty() }
      .toList()

    println("Xd")
    println(URI(documentId.uri).toPath().isFile())
    println(URI(documentId.uri).toPath().parent)
    println(documentIdInTheSameDirectoryToTargetsIdsMapForHACK)
    val targetsInTheSameDirectoryIfFile = getTargetsInTheSameDirectoryIfFileHACK(documentId)

    println(targetsInTheSameDirectoryIfFile)
    return targets + targetsInTheSameDirectoryIfFile
  }

  private fun getTargetsInTheSameDirectoryIfFileHACK(documentId: TextDocumentIdentifier): List<BuildTargetIdentifier> =
    when {
      isFileMap4HACK[documentId.uri] ?: false ->
        documentIdInTheSameDirectoryToTargetsIdsMapForHACK[URI(documentId.uri).toPath().parent].orEmpty().toList()

      else -> emptyList()
    }

  private fun generateAllDocumentSubdirectoriesIncludingDocument(documentId: TextDocumentIdentifier): Sequence<Path> {
    LOGGER.trace { "Generating all $documentId subdirectories..." }

    val documentAbsolutePath = mapDocumentIdToAbsolutePath(documentId)

    return documentAbsolutePath.allSubdirectoriesSequence()
      .also { LOGGER.trace { "Generating all $documentId subdirectories done! Subdirectories: $it." } }
  }

  private fun mapDocumentIdToAbsolutePath(documentId: TextDocumentIdentifier): Path =
    URI.create(documentId.uri).toAbsolutePath()

  private
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
    URI.create(sourceItem.uri).toAbsolutePath()

  companion object {
    private val LOGGER = logger<DocumentIdToTargetsIdsMapDelegate>()
  }
}

private class DocumentIdToTargetsIdsMapDelegateInTheSameDirHACK(private val sources: List<SourcesItem>) {

  operator fun getValue(
    thisRef: Any?,
    property: KProperty<*>,
  ): Map<Path, Set<BuildTargetIdentifier>> {
    return sources
      .flatMap(this::mapSourcesItemToPairsOfDocumentIdAndTargetId)
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.toSet() }
  }

  private fun mapSourcesItemToPairsOfDocumentIdAndTargetId(
    sourceItem: SourcesItem,
  ): List<Pair<Path, BuildTargetIdentifier>> =
    sourceItem.sources
      .mapNotNull { mapSourceItemToPath(it) }
      .map { Pair(it, sourceItem.target) }

  private fun mapSourceItemToPath(sourceItem: SourceItem): Path? = when (sourceItem.kind) {
    SourceItemKind.FILE -> URI.create(sourceItem.uri).toAbsolutePath().parent
    else -> null
  }
}
