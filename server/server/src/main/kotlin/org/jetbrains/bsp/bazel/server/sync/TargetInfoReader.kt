package org.jetbrains.bsp.bazel.server.sync

import com.google.protobuf.TextFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okio.IOException
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import java.nio.file.Path
import kotlin.io.path.reader

class TargetInfoReader(private val bspClientLogger: BspClientLogger) {
  suspend fun readTargetMapFromAspectOutputs(files: Set<Path>): Map<Label, TargetInfo> =
    withContext(Dispatchers.Default) {
      files.map { file -> async { readFromFile(file) } }.awaitAll()
    }.asSequence()
      .filterNotNull()
      .groupBy { it.id }
      // If any aspect has already been run on the build graph, it created shadow graph
      // containing new nodes of the same labels as the original ones. In particular,
      // this happens for all protobuf targets, for which a built-in aspect "bazel_java_proto_aspect"
      // is run. In order to correctly address this issue, we would have to provide separate
      // entities (TargetInfos) for each target and each ruleset (or language) instead of just
      // entity-per-label. As long as we don't have it, in case of a conflict we just take the entity
      // that contains JvmTargetInfo as currently it's the most important one for us. Later, we sort by
      // shortest size to get a stable result, which should be the default config.
      .mapValues {
        it.value.filter(TargetInfo::hasJvmTargetInfo).minByOrNull { targetInfo -> targetInfo.serializedSize } ?: it.value.first()
      }.mapKeys { Label.parse(it.key) }

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
      bspClientLogger.error("[WARN] Could not read target info $file: ${e.message}")
      return null
    }
    return builder.build()
  }
}
