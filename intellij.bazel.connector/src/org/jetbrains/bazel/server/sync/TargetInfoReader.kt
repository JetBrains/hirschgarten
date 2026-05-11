package org.jetbrains.bazel.server.sync

import com.google.protobuf.TextFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BazelTaskLogger
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.reader

internal class TargetInfoReader(private val taskLogger: BazelTaskLogger?) {
  suspend fun readTargetMapFromAspectOutputs(files: Set<Path>): Map<Label, TargetInfo> =
    withContext(Dispatchers.Default) {
      files.map { file -> async { readFromFile(file) } }.awaitAll()
    }.asSequence()
      .filterNotNull()
      .groupBy { it.key.label }
      // If any aspect has already been run on the build graph, it created shadow graph
      // containing new nodes of the same labels as the original ones. In particular,
      // this happens for all protobuf targets, for which a built-in aspect "bazel_java_proto_aspect"
      // is run. In order to correctly address this issue, we would have to provide separate
      // entities (TargetInfos) for each target and each ruleset (or language) instead of just
      // entity-per-label. As long as we don't have it, in case of a conflict we just take the entity
      // that contains JvmTargetInfo as currently it's the most important one for us.
      //
      // Among JVM nodes, prefer nodes with actual jar content. For java_proto_library the real
      // compilation artifacts live in a configuration-transition output directory (-ST-<hash>), and
      // the shadow graph may also produce a smaller, jar-less node for the same label. Picking the
      // jar-less node (as "smallest size") leaves the library empty and breaks IDE indexing.
      // Within each group we still sort by size for a stable result when all nodes are equivalent.
      .mapValues { resolveConflict(it.value) }
      .mapKeys { Label.parse(it.key) }

  companion object {
    /**
     * Selects the most representative [TargetInfo] when multiple nodes share the same label.
     *
     * Multiple nodes arise when Bazel's shadow graph is involved — most commonly for
     * `java_proto_library`, where the built-in `bazel_java_proto_aspect` produces additional
     * nodes in a configuration-transition output directory (`-ST-<hash>`). Among JVM nodes we
     * prefer those that actually contain jar artifacts, because the shadow-graph nodes without
     * jars are empty shells that would cause the library to be dropped from IntelliJ's classpath.
     * Within each preference tier we pick the smallest node for a stable, deterministic result.
     */
    internal fun resolveConflict(candidates: List<TargetInfo>): TargetInfo {
      val jvmTargets = candidates.filter { it.javaCommon.jvmTarget }
      return jvmTargets.filter { it.javaCommon.jarsList.isNotEmpty() }.minByOrNull { it.serializedSize }
        ?: jvmTargets.minByOrNull { it.serializedSize }
        ?: candidates.first()
    }
  }

  private fun readFromFile(file: Path): TargetInfo? {
    val builder = TargetInfo.newBuilder()
    val parser =
      TextFormat.Parser
        .newBuilder()
        .setAllowUnknownFields(true)
        .build()
    try {
      file.reader().use {
        parser.merge(it, builder)
      }
    } catch (e: IOException) {
      // Can happen if one output path is a prefix of another, then Bazel can't create both
      taskLogger?.error("[WARN] Could not read target info $file: ${e.message}")
      return null
    }
    return builder.build()
  }
}
