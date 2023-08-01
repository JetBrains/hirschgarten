package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import org.jetbrains.magicmetamodel.extensions.allSubdirectoriesSequence
import org.jetbrains.magicmetamodel.extensions.toAbsolutePath
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path

public data class TargetsDetailsForDocumentProviderState(
  public var documentIdToTargetsIdsMap: Map<String, Set<BuildTargetId>> = emptyMap(),
)

public class TargetsDetailsForDocumentProvider {

  private val documentIdToTargetsIdsMap: Map<Path, Set<BuildTargetId>>

  public constructor(sources: List<SourcesItem>) {
    log.trace { "Initializing TargetsDetailsForDocumentProvider with $sources..." }

    this.documentIdToTargetsIdsMap = DocumentIdToTargetsIdsMap(sources)

    log.trace { "Initializing TargetsDetailsForDocumentProvider done!" }
  }

  public constructor(state: TargetsDetailsForDocumentProviderState) {
    this.documentIdToTargetsIdsMap =
      state.documentIdToTargetsIdsMap.mapKeys { Path(it.key) }.mapValues { it.value }
  }

  public fun getAllDocuments(): List<TextDocumentIdentifier> =
    documentIdToTargetsIdsMap.keys
      .map { mapPathToTextDocumentIdentifier(it) }
      .toList()

  private fun mapPathToTextDocumentIdentifier(path: Path): TextDocumentIdentifier =
    TextDocumentIdentifier(path.toUri().toString())

  public fun getTargetsDetailsForDocument(documentId: TextDocumentIdentifier): Set<BuildTargetId> =
    generateAllDocumentSubdirectoriesIncludingDocument(documentId)
      .flatMap { documentIdToTargetsIdsMap[it].orEmpty() }
      .toSet()

  private fun generateAllDocumentSubdirectoriesIncludingDocument(documentId: TextDocumentIdentifier): Sequence<Path> {
    log.trace { "Generating all $documentId subdirectories..." }

    val documentAbsolutePath = mapDocumentIdToAbsolutePath(documentId)

    return documentAbsolutePath.allSubdirectoriesSequence()
      .also { log.trace { "Generating all $documentId subdirectories done! Subdirectories: $it." } }
  }

  private fun mapDocumentIdToAbsolutePath(documentId: TextDocumentIdentifier): Path =
    URI.create(documentId.uri).toAbsolutePath()

  public fun toState(): TargetsDetailsForDocumentProviderState =
    TargetsDetailsForDocumentProviderState(
      documentIdToTargetsIdsMap.mapKeys { it.key.toString() }
    )

  private companion object {
    private val log = logger<TargetsDetailsForDocumentProvider>()
  }
}

// TODO reverse sources
private object DocumentIdToTargetsIdsMap {

  private val log = logger<DocumentIdToTargetsIdsMap>()

  operator fun invoke(
    sources: List<SourcesItem>
  ): Map<Path, Set<BuildTargetId>> {
    log.trace { "Calculating document to target id map..." }

    return sources
      .flatMap { mapSourcesItemToPairsOfDocumentIdAndTargetId(it) }
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.toSet() }
      .also { log.trace { "Calculating document to target id map done! Map: $it." } }
  }

  private fun mapSourcesItemToPairsOfDocumentIdAndTargetId(
    sourceItem: SourcesItem,
  ): List<Pair<Path, BuildTargetId>> =
    sourceItem.sources
      .map { mapSourceItemToPath(it) }
      .map { Pair(it, sourceItem.target.uri) }

  private fun mapSourceItemToPath(sourceItem: SourceItem): Path =
    URI.create(sourceItem.uri).toAbsolutePath()
}
