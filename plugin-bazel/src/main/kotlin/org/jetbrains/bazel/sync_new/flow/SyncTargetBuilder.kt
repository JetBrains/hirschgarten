package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.impl.BazelGenericTargetData
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetDependency
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetEdge
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetResourceFile
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetSourceFile
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import org.jetbrains.bazel.sync_new.graph.impl.toBazelPath
import org.jetbrains.bsp.protocol.RawAspectTarget

class SyncTargetBuilder(
  private val project: Project,
  private val pathsResolver: BazelPathsResolver,
  private val tagsBuilder: SyncTagsBuilder = SyncTagsBuilder(),
) {
  fun buildTargetVertex(ctx: SyncContext, raw: RawAspectTarget): BazelTargetVertex {
    val vertexId = ctx.graph.getNextVertexId()
    return BazelTargetVertex(
      vertexId = vertexId,
      label = raw.target.label(),
      genericData = buildGeneralTargetData(raw),
    )
  }

  fun buildTargetEdge(ctx: SyncContext, from: ID, to: ID): BazelTargetEdge  {
    val edgeId = ctx.graph.getNextEdgeId()
    return BazelTargetEdge(
      edgeId = edgeId,
      from = from,
      to = to,
    )
  }

  private fun buildGeneralTargetData(raw: RawAspectTarget): BazelGenericTargetData {
    return BazelGenericTargetData(
      tags = tagsBuilder.build(raw),
      directDependencies = buildDependencyList(raw),
      sources = buildSourceList(raw),
      resources = buildResourceList(raw),
    )
  }

  private fun buildDependencyList(raw: RawAspectTarget): List<BazelTargetDependency> {
    return raw.target.dependenciesList
      .map { BazelTargetDependency(it.label()) }
  }

  private fun buildSourceList(raw: RawAspectTarget): List<BazelTargetSourceFile> {
    return raw.target.sourcesList
      .map {
        BazelTargetSourceFile(
          path = it.toBazelPath(),
          priority = 0,
        )
      }
  }

  private fun buildResourceList(raw: RawAspectTarget): List<BazelTargetResourceFile> {
    return raw.target.resourcesList
      .map {
        BazelTargetResourceFile(
          path = it.toBazelPath(),
        )
      }
  }
}
