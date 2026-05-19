package org.jetbrains.bazel.server.sync

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.aspect.lib.readTargetFromFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BazelTaskLogger
import java.nio.file.Path

internal class TargetInfoReader(private val taskLogger: BazelTaskLogger?) {
  suspend fun readTargetMapFromAspectOutputs(files: Set<Path>): Map<Label, TargetIdeInfo> =
    withContext(Dispatchers.Default) {
      files.map { file -> async {
        readTargetFromFile(file, { msg -> taskLogger?.error("Could not read target info $file: ${msg}") }) } }.awaitAll()
    }.asSequence()
      .filterNotNull()
      .groupBy { it.key.label }
      // If any aspect has already been run on the build graph, it created shadow graph
      // containing new nodes of the same labels as the original ones. In particular,
      // this happens for all protobuf targets, for which a built-in aspect "bazel_java_proto_aspect"
      // is run. In order to correctly address this issue, we would have to provide separate
      // entities (TargetInfos) for each target and each ruleset (or language) instead of just
      // entity-per-label. As long as we don't have it, in case of a conflict we just take the entity
      // that contains JvmTargetInfo as currently it's the most important one for us. Later, we sort by
      // shortest size to get a stable result, which should be the default config. For java toolchains,
      // we prefer the non-exec one.
      .mapValues {
        it.value.filter{ it.javaCommon.jvmTarget }.minByOrNull { targetInfo -> targetInfo.serializedSize }
        ?: it.value.filter { it.hasJavaToolchainInfo() && !it.javaToolchainInfo.isExecConfig }.minByOrNull { targetInfo -> targetInfo.serializedSize }
        ?: it.value.first()
      }.mapKeys { Label.parse(it.key) }
}
