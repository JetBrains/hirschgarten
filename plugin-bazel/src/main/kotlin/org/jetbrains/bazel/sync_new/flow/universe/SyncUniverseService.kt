package org.jetbrains.bazel.sync_new.flow.universe

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.connector.BazelConnector
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.QueryResult
import org.jetbrains.bazel.sync_new.connector.consistentLabels
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.connector.unwrapProtos
import org.jetbrains.bazel.sync_new.flow.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncProgressReporter
import org.jetbrains.bazel.sync_new.flow.SyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.SyncScope
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(
  Service.Level.PROJECT,
)
class SyncUniverseService(
  private val project: Project,
) {
  internal val universeState =
    project.storageContext.createFlatStore<SyncUniverseState>("bazel.sync.universe.state", StorageHints.USE_IN_MEMORY)
      .withCreator {
        SyncUniverseState(
          phase = SyncUniversePhase.BEFORE_FIRST_SYNC,
        )
      }
      .withCodec { ofKryo() }
      .build()

  val universe: SyncUniverseState
    get() = universeState.get()

  suspend fun computeUniverseDiff(ctx: SyncContext, scope: SyncScope, progress: SyncProgressReporter): SyncColdDiff {
    val connector = project.service<BazelConnectorService>().ofLegacyTask()
    if (scope.isFullSync) {
      universeState.reset()
    }

    val state = universeState.get()

    // clean init
    if (state.phase == SyncUniversePhase.BEFORE_FIRST_SYNC) {
      val universe = SyncUniverseImportBuilder.createUniverseImport(project)
      val repoMapping = fetchRepoMapping(progress)
      universeState.modify {
        SyncUniverseState(
          importState = universe,
          repoMapping = repoMapping,
          phase = SyncUniversePhase.AFTER_FIRST_SYNC,
        )
      }

      val targets = progress.task.withTask("querying_universe_targets", "Querying universe targets") {
        computeTargets(connector, universe)
      }
      return SyncColdDiff(added = targets)
    }

    val oldImportState = state.importState
    val newImportState = SyncUniverseImportBuilder.createUniverseImport(project)

    // import scope changed
    // compute target diff
    val diff = if (oldImportState.patterns != newImportState.patterns) {
      progress.task.withTask("querying_universe_targets", "Querying universe targets") {
        computeUniverseDiff(connector, oldImportState, newImportState)
      }
    } else {
      SyncColdDiff()
    }

    // internal repos changed
    // compute repo mappings
    val repoMapping = if (oldImportState.internalRepos != newImportState.internalRepos) {
      fetchRepoMapping(progress)
    } else {
      state.repoMapping
    }

    ctx.session.universeChanges = SyncUniverseChanges(
      hasDirectoriesChanged = oldImportState.directories != newImportState.directories
    )

    universeState.modify {
      it.copy(
        importState = newImportState,
        repoMapping = repoMapping,
      )
    }

    return diff
  }

  private suspend fun fetchRepoMapping(progress: SyncProgressReporter): SyncRepoMapping =
    progress.task.withTask("fetch_repo_mapping", "Fetching repo mapping") {
      LegacyBazelFrontendBridge.fetchRepoMapping(project)
    }

  private suspend fun computeUniverseDiff(
    connector: BazelConnector,
    oldState: SyncUniverseImportState,
    newState: SyncUniverseImportState,
  ): SyncColdDiff {
    // TODO: maybe we could avoid doing queries over entire universe
    //  and somehow optimize them, maybe using some symbolic optimizer
    val oldTargets = computeTargets(connector, oldState)
    val newTargets = computeTargets(connector, newState)

    val added = mutableSetOf<Label>()
    val removed = mutableSetOf<Label>()
    for (target in oldTargets + newTargets) {
      when {
        target in oldTargets && target !in newTargets -> removed.add(target)
        target !in oldTargets && target in newTargets -> added.add(target)
      }
    }

    return SyncColdDiff(
      added = added,
      removed = removed,
    )
  }

  private suspend fun computeTargets(connector: BazelConnector, state: SyncUniverseImportState): Set<Label> {
    val result = connector.query {
      defaults()
      keepGoing()
      consistentLabels()
      output(QueryOutput.LABEL)
      query(SyncUniverseQuery.createUniverseQuery(state.patterns))
    }
    return result.unwrap().unwrap<QueryResult.Labels>()
      .labels.toHashSet()
  }
}
