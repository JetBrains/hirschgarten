package org.jetbrains.magicmetamodel

import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import org.jetbrains.magicmetamodel.impl.PerformanceLogger
import org.jetbrains.magicmetamodel.impl.TargetsDetailsForDocumentProvider
import org.jetbrains.magicmetamodel.impl.TargetsDetailsForDocumentProviderState
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId

public data class MagicMetaModelTemporaryFacadeState(
  var targetsDetailsForDocumentProviderState: TargetsDetailsForDocumentProviderState =
    TargetsDetailsForDocumentProviderState(),
)

public class MagicMetaModelTemporaryFacade {
  private val targetsDetailsForDocumentProvider: TargetsDetailsForDocumentProvider

  public constructor(projectDetails: ProjectDetails) {
    this.targetsDetailsForDocumentProvider =
      PerformanceLogger.logPerformance("create-target-details-for-document-provider") {
        TargetsDetailsForDocumentProvider(projectDetails.sources)
      }
  }

  // only for OverlappingTargetsGraph - will be removed in the following PRs
  public constructor(targetsDetailsForDocumentProvider: TargetsDetailsForDocumentProvider) {
    this.targetsDetailsForDocumentProvider = targetsDetailsForDocumentProvider
  }

  public constructor(state: MagicMetaModelTemporaryFacadeState) {
    this.targetsDetailsForDocumentProvider =
      TargetsDetailsForDocumentProvider(state.targetsDetailsForDocumentProviderState)
  }

  // will be removed in the following PRs
  public fun allDocuments(): List<TextDocumentIdentifier> =
    targetsDetailsForDocumentProvider.getAllDocuments()

  // in the following PRs: TextDocumentIdentifier -> VirtualFile
  public fun getTargetsForFile(textDocument: TextDocumentIdentifier): Set<BuildTargetId> =
    targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(textDocument)

  public fun toState(): MagicMetaModelTemporaryFacadeState =
    MagicMetaModelTemporaryFacadeState(
      targetsDetailsForDocumentProviderState = targetsDetailsForDocumentProvider.toState(),
    )
}
