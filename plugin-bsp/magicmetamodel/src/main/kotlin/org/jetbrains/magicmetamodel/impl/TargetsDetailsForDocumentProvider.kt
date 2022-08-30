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
import kotlin.io.path.Path
import kotlin.io.path.toPath

public data class TargetsDetailsForDocumentProviderState(
  public var documentIdToTargetsIdsMap: Map<String, Set<BuildTargetIdentifierState>> = emptyMap(),
  public var documentIdInTheSameDirectoryToTargetsIdsMapForHACK: Map<String, Set<BuildTargetIdentifierState>> = emptyMap(),
  public var isFileMap4HACK: Map<String, Boolean> = emptyMap(),
)

internal class TargetsDetailsForDocumentProvider {

  private val documentIdToTargetsIdsMap: Map<Path, Set<BuildTargetIdentifier>>
  private val documentIdInTheSameDirectoryToTargetsIdsMapForHACK: Map<Path, Set<BuildTargetIdentifier>>
  private val isFileMap4HACK: Map<String, Boolean>

  constructor(sources: List<SourcesItem>) {
    log.trace { "Initializing TargetsDetailsForDocumentProvider with $sources..." }

    this.documentIdToTargetsIdsMap = DocumentIdToTargetsIdsMap(sources)
    this.documentIdInTheSameDirectoryToTargetsIdsMapForHACK = DocumentIdToTargetsIdsMapInTheSameDirHACK(sources)
    this.isFileMap4HACK = sources
      .flatMap { it.sources }
      .associateBy({ it.uri }, { it.kind == SourceItemKind.FILE })

    log.trace { "Initializing TargetsDetailsForDocumentProvider done!" }
  }

  constructor(state: TargetsDetailsForDocumentProviderState) {
    this.documentIdToTargetsIdsMap =
      state.documentIdToTargetsIdsMap.mapKeys { Path(it.key) }.mapValues { it.value.map { it.fromState() }.toSet() }
    this.documentIdInTheSameDirectoryToTargetsIdsMapForHACK =
      state.documentIdInTheSameDirectoryToTargetsIdsMapForHACK.mapKeys { Path(it.key) }
        .mapValues { it.value.map { it.fromState() }.toSet() }
    this.isFileMap4HACK = state.isFileMap4HACK
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

    val targetsInTheSameDirectoryIfFile = getTargetsInTheSameDirectoryIfFileHACK(documentId)

    return (targets + targetsInTheSameDirectoryIfFile).distinct()
  }

  private fun getTargetsInTheSameDirectoryIfFileHACK(documentId: TextDocumentIdentifier): List<BuildTargetIdentifier> =
    when {
      isFileMap4HACK[documentId.uri] ?: false ->
        documentIdInTheSameDirectoryToTargetsIdsMapForHACK[URI(documentId.uri).toPath().parent].orEmpty().toList()

      else -> ArrayList()
    }

  private fun generateAllDocumentSubdirectoriesIncludingDocument(documentId: TextDocumentIdentifier): Sequence<Path> {
    log.trace { "Generating all $documentId subdirectories..." }

    val documentAbsolutePath = mapDocumentIdToAbsolutePath(documentId)

    return documentAbsolutePath.allSubdirectoriesSequence()
      .also { log.trace { "Generating all $documentId subdirectories done! Subdirectories: $it." } }
  }

  private fun mapDocumentIdToAbsolutePath(documentId: TextDocumentIdentifier): Path =
    URI.create(documentId.uri).toAbsolutePath()

  fun toState(): TargetsDetailsForDocumentProviderState =
    TargetsDetailsForDocumentProviderState(
      documentIdToTargetsIdsMap.mapKeys { it.key.toString() }
        .mapValues { it.value.map { it.toState() }.toSet() },
      documentIdInTheSameDirectoryToTargetsIdsMapForHACK.mapKeys { it.key.toString() }
        .mapValues { it.value.map { it.toState() }.toSet() },
      isFileMap4HACK
    )

  companion object {
    private val log = logger<TargetsDetailsForDocumentProvider>()
  }
}

// TODO reverse sources
private object DocumentIdToTargetsIdsMap {

  private val log = logger<DocumentIdToTargetsIdsMap>()

  operator fun invoke(
    sources: List<SourcesItem>
  ): Map<Path, Set<BuildTargetIdentifier>> {
    log.trace { "Calculating document to target id map..." }

    return sources
      .flatMap(this::mapSourcesItemToPairsOfDocumentIdAndTargetId)
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.toSet() }
      .also { log.trace { "Calculating document to target id map done! Map: $it." } }
  }

  private fun mapSourcesItemToPairsOfDocumentIdAndTargetId(
    sourceItem: SourcesItem,
  ): List<Pair<Path, BuildTargetIdentifier>> =
    sourceItem.sources
      .map { mapSourceItemToPath(it) }
      .map { Pair(it, sourceItem.target) }

  private fun mapSourceItemToPath(sourceItem: SourceItem): Path =
    URI.create(sourceItem.uri).toAbsolutePath()
}

private object DocumentIdToTargetsIdsMapInTheSameDirHACK {

  operator fun invoke(
    sources: List<SourcesItem>
  ): Map<Path, Set<BuildTargetIdentifier>> =
    sources
      .flatMap { mapSourcesItemToPairsOfDocumentIdAndTargetId(it) }
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.toSet() }

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
