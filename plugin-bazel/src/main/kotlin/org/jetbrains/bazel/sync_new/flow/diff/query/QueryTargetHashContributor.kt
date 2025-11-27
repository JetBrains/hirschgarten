package org.jetbrains.bazel.sync_new.flow.diff.query

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.QueryResult
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.injectRepository
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.flow.diff.TargetHash
import org.jetbrains.bazel.sync_new.flow.diff.TargetHashContributor
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseTargetPattern
import org.jetbrains.bazel.ui.console.ids.PROJECT_SYNC_TASK_ID
import kotlin.io.path.absolutePathString

class QueryTargetHashContributor : TargetHashContributor {
  override suspend fun computeHashes(project: Project, patterns: List<SyncUniverseTargetPattern>): Sequence<TargetHash> {
    val connector = project.serviceAsync<BazelConnectorService>()
      .ofLegacyTask(taskId = PROJECT_SYNC_TASK_ID)

    val workspaceContext = LegacyBazelFrontendBridge.fetchWorkspaceContext(project)
    val result = connector.query {
      defaults()
      keepGoing()
      output(QueryOutput.STREAMED_PROTO)
      injectRepository("bazelbsp_aspect=${workspaceContext.dotBazelBspDirPath.absolutePathString()}")
      query(QueryTargetPattern.createUniverseQuery(patterns))
    }

    val queryResult = result.unwrap()
    val list = when (queryResult) {
      is QueryResult.Proto -> {
        queryResult.result.targetList
          .asFlow()
      }

      is QueryResult.StreamedProto -> {
        queryResult.flow
      }

      else -> error("unsupported")
    }
    return list
      .mapNotNull {
        val rule = it.getRuleOrNull() ?: return@mapNotNull null
        TargetHash(
          target = Label.parse(rule.name),
          hash = BuildRuleProtoHasher.hash(rule)
        )
      }
      .toList()
      .asSequence()
  }

  private fun Build.Target.getRuleOrNull() = when (type) {
    Build.Target.Discriminator.RULE -> rule
    else -> null
  }

}
