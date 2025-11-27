package org.jetbrains.bazel.sync_new.bridge

import com.google.common.collect.HashBiMap
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync_new.flow.BzlmodSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.DisabledSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.SyncRepoMapping
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.RawAspectTarget
import org.jetbrains.bsp.protocol.WorkspaceBuildPartialTargetsParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector

object LegacyBazelFrontendBridge {
  suspend fun fetchRepoMapping(project: Project): SyncRepoMapping {
    val mapping = project.connection.runWithServer { server -> server.workspaceComputeBazelRepoMapping() }.repoMapping
    return when (mapping) {
      is BzlmodRepoMapping -> BzlmodSyncRepoMapping(
        canonicalRepoNameToLocalPath = mapping.canonicalRepoNameToLocalPath,
        apparentToCanonical = HashBiMap.create(mapping.apparentRepoNameToCanonicalName),
        canonicalToPath = mapping.canonicalRepoNameToPath,
      )

      RepoMappingDisabled -> DisabledSyncRepoMapping
    }
  }

  suspend fun fetchWorkspaceContext(project: Project): WorkspaceContext {
    return project.connection.runWithServer { server -> server.workspaceContext() }
  }

  //suspend fun fetchAllTargets(project: Project, repoMapping: RepoMapping): List<RawAspectTarget> {
  //  val params = WorkspaceBuildTargetParams(
  //    selector = WorkspaceBuildTargetSelector.AllTargets
  //  )
  //  val result = project.connection.runWithServer { server -> server.workspaceBuildTargets(params) }
  //  return result.targets.values.toList()
  //}

  suspend fun fetchPartialTargets(project: Project, repoMapping: SyncRepoMapping, targets: List<Label>): List<RawAspectTarget> {
    val params = WorkspaceBuildPartialTargetsParams(
      targets = targets,
      repoMapping = toLegacyRepoMapping(repoMapping),
    )
    val result = project.connection.runWithServer { server -> server.workspaceBuildTargetsPartial(params) }
    return result.targets.values
      .map { RawAspectTarget(it) }
  }

  suspend fun fetchBazelPathsResolver(project: Project): BazelPathsResolver {
    return project.connection.runWithServer { server -> server.workspaceBazelPaths().bazelPathsResolver }
  }

  fun toLegacyRepoMapping(repoMapping: SyncRepoMapping): RepoMapping {
    return when (repoMapping) {
      is BzlmodSyncRepoMapping -> BzlmodRepoMapping(
        canonicalRepoNameToLocalPath = repoMapping.canonicalRepoNameToLocalPath,
        apparentRepoNameToCanonicalName = BidirectionalMap.getTypedInstance<String, String>()
          .apply { putAll(repoMapping.apparentToCanonical) },
        canonicalRepoNameToPath = repoMapping.canonicalToPath,
      )

      DisabledSyncRepoMapping -> RepoMappingDisabled
    }
  }

}
